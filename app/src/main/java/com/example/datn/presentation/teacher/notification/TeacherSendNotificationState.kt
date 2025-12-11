package com.example.datn.presentation.teacher.notification

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.User

data class TeacherSendNotificationState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val students: List<User> = emptyList(),
    val successMessage: String? = null
) : BaseState
