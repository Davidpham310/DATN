package com.example.datn.presentation.common.notification

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Notification

data class NotificationState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val notifications: List<Notification> = emptyList(),
    val successMessage: String? = null
) : BaseState
