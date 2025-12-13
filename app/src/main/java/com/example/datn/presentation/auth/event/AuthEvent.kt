package com.example.datn.presentation.auth.event

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.UserRole

sealed class AuthEvent : BaseEvent {
    data class OnLogin(val email: String, val password: String , val role : UserRole) : AuthEvent()
    data class OnRegister(val email: String, val password: String, val name: String, val role: UserRole) : AuthEvent()
    data class OnForgotPassword(val email: String, val role: UserRole) : AuthEvent()
}