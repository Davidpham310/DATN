package com.example.datn.core.presentation.notifications

data class NotificationState(
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val isVisible: Boolean = false,
    val duration: Long = 3000L,
    val isQuestion: Boolean = false, // true = hiển thị nút
    val onConfirm: (() -> Unit)? = null, // callback nút 1
    val onCancel: (() -> Unit)? = null   // callback nút 2
)

enum class NotificationType {
    SUCCESS, ERROR, INFO
}