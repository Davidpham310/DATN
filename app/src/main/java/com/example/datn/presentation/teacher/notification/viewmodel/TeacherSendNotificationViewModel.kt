package com.example.datn.presentation.teacher.notification.viewmodel

import android.util.Log
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.usecase.notification.SendNotificationToStudentAndParentUseCase
import com.example.datn.domain.usecase.notification.SendNotificationParams
import com.example.datn.domain.usecase.student.GetStudentsUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.teacher.notification.event.TeacherSendNotificationEvent
import com.example.datn.presentation.teacher.notification.state.TeacherSendNotificationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class TeacherSendNotificationViewModel @Inject constructor(
    private val getStudents: GetStudentsUseCase,
    private val sendNotification: SendNotificationToStudentAndParentUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<TeacherSendNotificationState, TeacherSendNotificationEvent>(
    TeacherSendNotificationState(),
    notificationManager
) {

    init {
        loadStudents()
    }

    override fun onEvent(event: TeacherSendNotificationEvent) {
        when (event) {
            is TeacherSendNotificationEvent.SendNotification -> sendNotificationToStudent(event)
            is TeacherSendNotificationEvent.ClearMessages -> setState { copy(successMessage = null, error = null) }
        }
    }

    private fun loadStudents() {
        Log.d("TeacherSendNotificationViewModel", "Loading students...")
        launch {
            // This would need to be implemented based on your actual use case
            // For now, we'll set an empty list
            setState { copy(students = emptyList()) }
        }
    }

    private fun sendNotificationToStudent(event: TeacherSendNotificationEvent.SendNotification) {
        Log.d("TeacherSendNotificationViewModel", "Sending notification to student: ${event.studentId}")
        launch {
            // Get current user ID (teacher ID)
            val teacherId = "current_teacher_id" // This should come from auth
            
            val params = SendNotificationParams(
                teacherId = teacherId,
                studentId = event.studentId,
                parentId = null,
                type = event.type,
                title = event.title,
                content = event.content
            )

            sendNotification(params).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d("TeacherSendNotificationViewModel", "Sending notification...")
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        Log.d("TeacherSendNotificationViewModel", "Notification sent successfully")
                        setState {
                            copy(
                                isLoading = false,
                                successMessage = "Gửi thông báo thành công",
                                error = null
                            )
                        }
                        showNotification("Gửi thông báo thành công", NotificationType.SUCCESS)
                    }
                    is Resource.Error -> {
                        Log.e("TeacherSendNotificationViewModel", "Error sending notification: ${result.message}")
                        setState {
                            copy(
                                isLoading = false,
                                error = result.message ?: "Lỗi gửi thông báo",
                                successMessage = null
                            )
                        }
                        showNotification(result.message ?: "Lỗi gửi thông báo", NotificationType.ERROR)
                    }
                }
            }
        }
    }
}
