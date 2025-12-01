package com.example.datn.presentation.student.lessons

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.IProgressRepository
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
    private val progressRepository: IProgressRepository,
    notificationManager: NotificationManager
) : BaseViewModel<StudentLessonViewState, StudentLessonViewEvent>(
    StudentLessonViewState(),
    notificationManager
) {

    companion object {
        private const val TAG = "StudentLessonViewVM"
        private const val AUTO_SAVE_INTERVAL = 10000L
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
            is StudentLessonViewEvent.LoadLesson -> loadLesson(event.lessonId, event.initialContentId)
            StudentLessonViewEvent.NextContent -> navigateToNextContent()
            StudentLessonViewEvent.PreviousContent -> navigateToPreviousContent()
            is StudentLessonViewEvent.GoToContent -> navigateToContent(event.index)
            StudentLessonViewEvent.MarkCurrentAsViewed -> markCurrentContentAsViewed()
            StudentLessonViewEvent.ShowProgressDialog -> setState { copy(showProgressDialog = true) }
            StudentLessonViewEvent.DismissProgressDialog -> setState { copy(showProgressDialog = false) }
            StudentLessonViewEvent.SaveProgress -> saveProgress()
        }
    }

    private fun loadLesson(lessonId: String, initialContentId: String?) {
        viewModelScope.launch {
            Log.d(TAG, "loadLesson() called with lessonId=$lessonId")
            setState {
                copy(
                    isLoading = true,
                    error = null,
                    lessonId = lessonId,
                    sessionStartTime = System.currentTimeMillis()
                )
            }

            // Resolve studentId once when loading lesson so we can restore existing progress
            var resolvedStudentId: String? = null
            try {
                val currentUserId = currentUserIdFlow.value.ifBlank {
                    currentUserIdFlow.first { it.isNotBlank() }
                }
                if (currentUserId.isBlank()) {
                    Log.e(TAG, "loadLesson() aborted resolving studentId: currentUserId is blank")
                    showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                } else {
                    val profileResult = getStudentProfileByUserId(currentUserId)
                        .first { it !is Resource.Loading }
                    when (profileResult) {
                        is Resource.Success -> {
                            resolvedStudentId = profileResult.data?.id
                            if (resolvedStudentId.isNullOrBlank()) {
                                Log.e(TAG, "loadLesson() failed: studentId is null/blank in profile")
                                showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                            } else {
                                setState { copy(studentId = resolvedStudentId) }
                            }
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "loadLesson() failed to resolve studentId: ${profileResult.message}")
                            showNotification(profileResult.message, NotificationType.ERROR)
                        }
                        is Resource.Loading -> {
                            // ignored by first { it !is Resource.Loading }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadLesson() exception while resolving studentId", e)
            }

            val studentId = resolvedStudentId

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
                        val contents = contentsResult.data?.sortedBy { it.order } ?: emptyList()
                        setState {
                            copy(
                                lesson = lessonResult.data,
                                lessonContents = contents,
                                currentContentIndex = 0,
                                isLoading = false,
                                error = null
                            )
                        }
                        // Resolve direct URLs for non-text contents similar to teacher loadDirectContentUrl
                        contents.forEach { content ->
                            if (content.contentType != ContentType.TEXT && content.content.isNotEmpty()) {
                                loadDirectContentUrl(content)
                            }
                        }
                        // If there is existing progress for this student and lesson, restore it
                        if (!studentId.isNullOrBlank()) {
                            loadExistingProgressForLesson(studentId, lessonId)
                        }

                        // If a specific contentId is requested (detail route), navigate to it first
                        if (!initialContentId.isNullOrBlank()) {
                            navigateToContentById(initialContentId)
                        }

                        // Automatically mark content as viewed + save progress ONLY for non-video contents.
                        // Video contents will be marked as viewed explicitly from the UI after the student
                        // has watched enough (≥80%) and confirmed in the player overlay.
                        val currentContent = state.value.currentContent
                        if (currentContent != null && currentContent.contentType != ContentType.VIDEO) {
                            markCurrentContentAsViewed()
                            saveProgress()
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

    private fun loadDirectContentUrl(content: LessonContent) {
        viewModelScope.launch {
            lessonUseCases.getDirectLessonContentUrl(content.content.trimStart('/'))
                .collectLatest { result ->
                    when (result) {
                        is Resource.Loading -> {
                            // background URL loading, không đổi isLoading chung để tránh nháy UI
                        }
                        is Resource.Success -> {
                            val url = result.data
                            if (!url.isNullOrBlank()) {
                                setState {
                                    copy(
                                        contentUrls = contentUrls + (content.id to url)
                                    )
                                }
                            } else {
                                showNotification("Không tìm thấy URL cho nội dung", NotificationType.ERROR)
                            }
                        }
                        is Resource.Error -> {
                            showNotification("Lỗi khi tải URL: ${result.message}", NotificationType.ERROR)
                        }
                    }
                }
        }
    }

    private suspend fun loadExistingProgressForLesson(
        studentId: String,
        lessonId: String
    ) {
        try {
            val result = progressRepository
                .getLessonProgress(studentId, lessonId)
                .first { it !is Resource.Loading }

            when (result) {
                is Resource.Success -> {
                    val existing = result.data ?: return
                    val contents = state.value.lessonContents
                    if (contents.isEmpty()) {
                        setState {
                            copy(
                                progress = existing,
                                studentId = studentId,
                                lessonId = lessonId
                            )
                        }
                        return
                    }

                    val totalContents = contents.size
                    val percentage = existing.progressPercentage.coerceIn(0, 100)
                    val viewedCount = (percentage * totalContents) / 100
                    val safeViewedCount = viewedCount.coerceIn(0, totalContents)

                    val initialViewedIds = contents
                        .take(safeViewedCount)
                        .map { it.id }
                        .toMutableSet()

                    val lastContentIndex = existing.lastAccessedContentId?.let { lastId ->
                        contents.indexOfFirst { it.id == lastId }
                    } ?: -1

                    val targetIndex = when {
                        lastContentIndex in contents.indices -> lastContentIndex
                        safeViewedCount > 0 -> safeViewedCount - 1
                        else -> 0
                    }.coerceIn(0, contents.lastIndex)

                    if (targetIndex in contents.indices) {
                        initialViewedIds.add(contents[targetIndex].id)
                    }

                    setState {
                        copy(
                            progress = existing,
                            studentId = studentId,
                            lessonId = lessonId,
                            currentContentIndex = targetIndex,
                            viewedContentIds = initialViewedIds
                        )
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "loadExistingProgressForLesson() error: ${result.message}")
                }
                is Resource.Loading -> {
                    // handled by first { it !is Resource.Loading }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadExistingProgressForLesson() exception", e)
        }
    }

    private fun getMaxAccessibleIndex(state: StudentLessonViewState): Int {
        val contents = state.lessonContents
        if (contents.isEmpty()) return -1
        if (state.viewedContentIds.isEmpty()) return 0

        val lastViewedIndex = contents.indices
            .filter { contents[it].id in state.viewedContentIds }
            .maxOrNull() ?: 0

        return (lastViewedIndex + 1).coerceAtMost(contents.lastIndex)
    }

    private fun navigateToNextContent() {
        val current = state.value
        if (current.canGoNext) {
            val maxAccessibleIndex = getMaxAccessibleIndex(current)
            val newIndex = current.currentContentIndex + 1
            if (newIndex > maxAccessibleIndex) {
                return
            }
            val targetContent = current.lessonContents.getOrNull(newIndex) ?: return
            setState {
                copy(
                    currentContentIndex = newIndex,
                    viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                        viewedContentIds + targetContent.id
                    } else {
                        viewedContentIds
                    }
                )
            }
        }
    }
    
    private fun navigateToPreviousContent() {
        val current = state.value
        if (current.canGoPrevious) {
            val newIndex = current.currentContentIndex - 1
            val targetContent = current.lessonContents.getOrNull(newIndex) ?: return
            setState {
                copy(
                    currentContentIndex = newIndex,
                    viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                        viewedContentIds + targetContent.id
                    } else {
                        viewedContentIds
                    }
                )
            }
        }
    }
    
    private fun navigateToContent(index: Int) {
        val current = state.value
        val maxAccessibleIndex = getMaxAccessibleIndex(current)
        if (index !in current.lessonContents.indices || index > maxAccessibleIndex) {
            return
        }
        val targetContent = current.lessonContents[index]
        setState {
            copy(
                currentContentIndex = index,
                viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                    viewedContentIds + targetContent.id
                } else {
                    viewedContentIds
                }
            )
        }
    }
    
    private fun navigateToContentById(contentId: String) {
        val current = state.value
        val index = current.lessonContents.indexOfFirst { it.id == contentId }
        if (index < 0) {
            return
        }

        val maxAccessibleIndex = getMaxAccessibleIndex(current)
        if (index > maxAccessibleIndex) {
            return
        }

        val targetContent = current.lessonContents[index]
        setState {
            copy(
                currentContentIndex = index,
                viewedContentIds = if (targetContent.contentType != ContentType.VIDEO) {
                    viewedContentIds + targetContent.id
                } else {
                    viewedContentIds
                }
            )
        }
    }
    
    private fun markCurrentContentAsViewed() {
        val current = state.value
        val currentContent = current.currentContent ?: return
        Log.d(TAG, "markCurrentContentAsViewed() contentId=${currentContent.id}")
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

            Log.d(
                TAG,
                "saveProgress() started for lessonId=$lessonId, currentIndex=${currentState.currentContentIndex}, progress=${currentState.progressPercentage}%"
            )

            // Resolve studentId using cached current user ID and student profile
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.first { it.isNotBlank() }
            }
            if (currentUserId.isBlank()) {
                Log.e(TAG, "saveProgress() aborted: currentUserId is blank")
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            var resolvedStudentId: String? = currentState.studentId
            if (resolvedStudentId.isNullOrBlank()) {
                Log.d(TAG, "saveProgress() resolving studentId for userId=$currentUserId from profile")
                getStudentProfileByUserId(currentUserId).collectLatest { profileResult ->
                    when (profileResult) {
                        is Resource.Success -> {
                            resolvedStudentId = profileResult.data?.id
                            Log.d(TAG, "saveProgress() resolved studentId=${resolvedStudentId}")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "saveProgress() failed to resolve studentId: ${profileResult.message}")
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
                Log.e(TAG, "saveProgress() aborted: studentId is null/blank after resolving")
                showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                return@launch
            }

            val now = System.currentTimeMillis()
            val sessionStart = currentState.sessionStartTime.takeIf { it > 0 } ?: now
            val additionalSeconds = ((now - sessionStart) / 1000).coerceAtLeast(0)

            val progressPercentage = currentState.progressPercentage
            val lastContentId = currentState.currentContent?.id

            Log.d(
                TAG,
                "saveProgress() preparing update: studentId=$studentId, lessonId=$lessonId, additionalSeconds=$additionalSeconds, progress=$progressPercentage, lastContentId=$lastContentId"
            )

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
                        Log.d(TAG, "saveProgress() -> updateLessonProgress: Loading")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "saveProgress() -> updateLessonProgress: Success, progressId=${result.data?.id}")
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
                        Log.e(TAG, "saveProgress() -> updateLessonProgress: Error=${result.message}")
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Không thể lưu tiến độ bài học", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        Log.d(TAG, "startAutoSave() called, interval=${AUTO_SAVE_INTERVAL}ms")
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_SAVE_INTERVAL)
                Log.d(TAG, "auto-save tick -> calling saveProgress()")
                saveProgress()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared() called, cancelling autoSaveJob and forcing final saveProgress()")
        autoSaveJob?.cancel()
        viewModelScope.launch {
            Log.d(TAG, "onCleared() -> final saveProgress()")
            saveProgress()
        }
    }
}
