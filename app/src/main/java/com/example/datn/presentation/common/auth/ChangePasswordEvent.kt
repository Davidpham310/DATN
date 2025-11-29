package com.example.datn.presentation.common.auth

import com.example.datn.core.base.BaseEvent

sealed class ChangePasswordEvent : BaseEvent {
    data class OnChangePassword(
        val currentPassword: String,
        val newPassword: String,
        val confirmPassword: String
    ) : ChangePasswordEvent()

    object ClearMessages : ChangePasswordEvent()
}
