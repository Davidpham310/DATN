package com.example.datn.presentation.common.notification

import com.example.datn.core.base.BaseEvent

sealed class NotificationEvent : BaseEvent {
    data class LoadNotifications(val userId: String) : NotificationEvent()
    data class MarkAsRead(val notificationId: String) : NotificationEvent()
    object ClearMessages : NotificationEvent()
}
