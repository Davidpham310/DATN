package com.example.datn.presentation.student.lessons.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.lesson.GetLessonContentsRequest
import com.example.datn.domain.usecase.lesson.LessonUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.student.lessons.event.StudentLessonContentListEvent
import com.example.datn.presentation.student.lessons.state.StudentLessonContentListState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
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
                    awaitNonBlank(currentUserIdFlow)
                }
                if (currentUserId.isBlank()) {
                    setState { copy(isLoading = false, error = "Vui lòng đăng nhập") }
                    showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                    return@launch
                }

                val profileResult = awaitFirstNonLoading(getStudentProfileByUserId(currentUserId))

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
                showNotification(e.message ?: "Lỗi tải nội dung bài học", NotificationType.ERROR)
            }
        }
    }

    private suspend fun awaitNonBlank(flow: Flow<String>): String {
        var result = ""
        flow
            .filter { it.isNotBlank() }
            .take(1)
            .collect { value -> result = value }
        return result
    }

    private suspend fun <T> awaitFirstNonLoading(flow: Flow<Resource<T>>): Resource<T> {
        var result: Resource<T>? = null
        flow
            .filter { it !is Resource.Loading }
            .take(1)
            .collect { value -> result = value }
        return result ?: Resource.Error("Không thể tải dữ liệu")
    }
}
