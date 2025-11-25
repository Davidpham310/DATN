package com.example.datn.presentation.student.lessons

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.lesson.GetLessonContentsRequest
import com.example.datn.domain.usecase.lesson.LessonUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StudentLessonContentListViewModel @Inject constructor(
    private val lessonUseCases: LessonUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentLessonContentListState, StudentLessonContentListEvent>(
    StudentLessonContentListState(),
    notificationManager
) {

    // Cache current user ID to avoid timing issues similar to StudentClassViewModel
    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    override fun onEvent(event: StudentLessonContentListEvent) {
        when (event) {
            is StudentLessonContentListEvent.LoadLesson -> loadLesson(event.lessonId)
        }
    }

    private fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            try {
                val currentUserId = currentUserIdFlow.value.ifBlank {
                    currentUserIdFlow.first { it.isNotBlank() }
                }
                if (currentUserId.isBlank()) {
                    setState { copy(isLoading = false, error = "Vui lòng đăng nhập") }
                    showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                    return@launch
                }

                val profileResult = getStudentProfileByUserId(currentUserId)
                    .first { it !is Resource.Loading }

                val studentId = when (profileResult) {
                    is Resource.Success -> profileResult.data?.id
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = profileResult.message) }
                        showNotification(profileResult.message, NotificationType.ERROR)
                        return@launch
                    }
                    else -> null
                }

                if (studentId.isNullOrBlank()) {
                    setState { copy(isLoading = false, error = "Không tìm thấy thông tin học sinh") }
                    showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                    return@launch
                }

                lessonUseCases.getLessonContents(
                    GetLessonContentsRequest(
                        studentId = studentId,
                        lessonId = lessonId
                    )
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Loading -> {
                            setState { copy(isLoading = true) }
                        }
                        is Resource.Success -> {
                            val data = result.data
                            setState {
                                copy(
                                    lesson = data?.lesson,
                                    contents = data?.contents ?: emptyList(),
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            setState {
                                copy(
                                    isLoading = false,
                                    error = result.message
                                )
                            }
                            showNotification(result.message, NotificationType.ERROR)
                        }
                    }
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                showNotification(e.message ?: "Đã xảy ra lỗi", NotificationType.ERROR)
            }
        }
    }
}
