package com.example.datn.presentation.teacher.notification.viewmodel

import com.example.datn.core.base.BaseEvent
import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Notification

data class TeacherNotificationListState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val userId: String? = null,
    val notifications: List<Notification> = emptyList()
) : BaseState

sealed class TeacherNotificationListEvent : BaseEvent {
    object Load : TeacherNotificationListEvent()
    object Refresh : TeacherNotificationListEvent()
}
