package com.example.datn.core.presentation.notifications

data class NotificationState(
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val isVisible: Boolean = false,
    val duration: Long = 3000L,
)

enum class NotificationType {
    SUCCESS, ERROR, INFO
}