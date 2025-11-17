package com.example.datn.presentation.student.lessons

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.lesson.LessonUseCases
import com.example.datn.domain.usecase.progress.UpdateLessonProgressParams
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentLessonViewViewModel @Inject constructor(
    private val lessonUseCases: LessonUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentLessonViewState, StudentLessonViewEvent>(
    StudentLessonViewState(),
    notificationManager
) {

    companion object {
        private const val AUTO_SAVE_INTERVAL = 60000L
    }

    // Cache current user ID to avoid timing issues
    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    private var autoSaveJob: Job? = null

    override fun onEvent(event: StudentLessonViewEvent) {
        when (event) {
            is StudentLessonViewEvent.LoadLesson -> loadLesson(event.lessonId)
            StudentLessonViewEvent.NextContent -> navigateToNextContent()
            StudentLessonViewEvent.PreviousContent -> navigateToPreviousContent()
            is StudentLessonViewEvent.GoToContent -> navigateToContent(event.index)
            StudentLessonViewEvent.MarkCurrentAsViewed -> markCurrentContentAsViewed()
            StudentLessonViewEvent.ShowProgressDialog -> setState { copy(showProgressDialog = true) }
            StudentLessonViewEvent.DismissProgressDialog -> setState { copy(showProgressDialog = false) }
            StudentLessonViewEvent.SaveProgress -> saveProgress()
        }
    }

    private fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            setState {
                copy(
                    isLoading = true,
                    error = null,
                    lessonId = lessonId,
                    sessionStartTime = System.currentTimeMillis()
                )
            }

            combine(
                lessonUseCases.getLessonById(lessonId),
                lessonUseCases.getLessonContentsByLesson(lessonId)
            ) { lessonResult, contentsResult ->
                Pair(lessonResult, contentsResult)
            }.collectLatest { (lessonResult, contentsResult) ->
                when {
                    lessonResult is Resource.Loading || contentsResult is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    lessonResult is Resource.Success && contentsResult is Resource.Success -> {
                        setState {
                            copy(
                                lesson = lessonResult.data,
                                lessonContents = contentsResult.data?.sortedBy { it.order } ?: emptyList(),
                                currentContentIndex = 0,
                                isLoading = false,
                                error = null
                            )
                        }
                        startAutoSave()
                    }
                    lessonResult is Resource.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = lessonResult.message
                            )
                        }
                        showNotification(lessonResult.message, NotificationType.ERROR)
                    }
                    contentsResult is Resource.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = contentsResult.message
                            )
                        }
                        showNotification(contentsResult.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }
    
    private fun navigateToNextContent() {
        val state = state.value
        if (state.canGoNext) {
            setState { copy(currentContentIndex = currentContentIndex + 1) }
        }
    }
    
    private fun navigateToPreviousContent() {
        val state = state.value
        if (state.canGoPrevious) {
            setState { copy(currentContentIndex = currentContentIndex - 1) }
        }
    }
    
    private fun navigateToContent(index: Int) {
        val state = state.value
        if (index in state.lessonContents.indices) {
            setState { copy(currentContentIndex = index) }
        }
    }
    
    private fun markCurrentContentAsViewed() {
        val state = state.value
        val currentContent = state.currentContent ?: return
        setState {
            copy(viewedContentIds = viewedContentIds + currentContent.id)
        }
    }
    
    private fun saveProgress() {
        viewModelScope.launch {
            val currentState = state.value
            val lesson = currentState.lesson ?: return@launch

            // Resolve lessonId from state or lesson
            val lessonId = currentState.lessonId ?: lesson.id

            // Resolve studentId using cached current user ID and student profile
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.first { it.isNotBlank() }
            }
            if (currentUserId.isBlank()) {
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            var resolvedStudentId: String? = currentState.studentId
            if (resolvedStudentId.isNullOrBlank()) {
                getStudentProfileByUserId(currentUserId).collectLatest { profileResult ->
                    when (profileResult) {
                        is Resource.Success -> {
                            resolvedStudentId = profileResult.data?.id
                        }
                        is Resource.Error -> {
                            showNotification(profileResult.message, NotificationType.ERROR)
                        }
                        is Resource.Loading -> {
                            // ignore
                        }
                    }
                }
            }

            val studentId = resolvedStudentId
            if (studentId.isNullOrBlank()) {
                showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                return@launch
            }

            val now = System.currentTimeMillis()
            val sessionStart = currentState.sessionStartTime.takeIf { it > 0 } ?: now
            val additionalSeconds = ((now - sessionStart) / 1000).coerceAtLeast(0)

            val progressPercentage = currentState.progressPercentage
            val lastContentId = currentState.currentContent?.id

            val params = UpdateLessonProgressParams(
                studentId = studentId,
                lessonId = lessonId,
                progressPercentage = progressPercentage,
                lastAccessedContentId = lastContentId,
                additionalTimeSeconds = additionalSeconds
            )

            lessonUseCases.updateLessonProgress(params).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        setState {
                            copy(
                                progress = result.data,
                                studentId = studentId,
                                lessonId = lessonId,
                                isLoading = false,
                                // Reset session start to now for next tracking window
                                sessionStartTime = now
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Không thể lưu tiến độ bài học", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_SAVE_INTERVAL)
                saveProgress()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        viewModelScope.launch {
            saveProgress()
        }
    }
}
