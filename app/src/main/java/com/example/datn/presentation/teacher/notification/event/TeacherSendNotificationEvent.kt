package com.example.datn.presentation.teacher.notification.event

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.NotificationType

sealed class TeacherSendNotificationEvent : BaseEvent {
    data class SendNotification(
        val studentId: String,
        val title: String,
        val content: String,
        val type: NotificationType
    ) : TeacherSendNotificationEvent()
    
    object ClearMessages : TeacherSendNotificationEvent()
}
