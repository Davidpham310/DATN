package com.example.datn.presentation.common.auth

import com.example.datn.core.base.BaseEvent

sealed class AuthEvent : BaseEvent {
    data class OnLogin(val email: String, val password: String) : AuthEvent()
    data class OnRegister(val email: String, val password: String, val name: String, val role: String) : AuthEvent()
    data class OnForgotPassword(val email: String) : AuthEvent()
}