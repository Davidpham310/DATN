package com.example.datn.core.presentation.notifications

sealed class NotificationEvent {
    data class Show(val message: String, val type: NotificationType, val duration: Long = 3000L , val isQuestion: Boolean = false, val onConfirm: (() -> Unit)? = null, val onCancel: (() -> Unit)? = null) : NotificationEvent()
    object Dismiss : NotificationEvent()
}