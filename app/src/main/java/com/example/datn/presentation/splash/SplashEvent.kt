package com.example.datn.presentation.splash

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.UserRole

sealed class SplashEvent : BaseEvent {
    object CheckCurrentUser : SplashEvent()
    data class NavigateToHome(val role: UserRole) : SplashEvent()
    object NavigateToLogin : SplashEvent()
}