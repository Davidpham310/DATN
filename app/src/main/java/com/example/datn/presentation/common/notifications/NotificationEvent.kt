package com.example.datn.presentation.common.notifications

sealed class NotificationEvent {
    data class Show(val message: String, val type: NotificationType, val duration: Long = 3000L) : NotificationEvent()
    object Dismiss : NotificationEvent()
}
