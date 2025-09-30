package com.example.datn.presentation.auth

import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.auth.ForgotPasswordParams
import com.example.datn.domain.usecase.auth.LoginParams
import com.example.datn.domain.usecase.auth.RegisterParams
import com.example.datn.presentation.common.AuthEvent
import com.example.datn.presentation.common.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : BaseViewModel<AuthState, AuthEvent>(AuthState()) {

    override fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.OnLogin -> login(event.email, event.password)
            is AuthEvent.OnRegister -> register(event.email, event.password, event.name, event.role)
            is AuthEvent.OnForgotPassword -> forgotPassword(event.email)
        }
    }

    private fun login(email: String, password: String) {
        launch {
            authUseCases.login(LoginParams(email, password)).collect { resource ->
                handleAuthResult(resource, successMessage = "Login successful")
            }
        }
    }

    private fun register(email: String, password: String, name: String, role: String) {
        launch {
            authUseCases.register(RegisterParams(email, password, name, role)).collect { resource ->
                handleAuthResult(resource, successMessage = "Register successful")
            }
        }
    }

    private fun forgotPassword(email: String) {
        launch {
            authUseCases.forgotPassword(ForgotPasswordParams(email)).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, error = null) }
                        showNotification(resource.data ?: "Password reset email sent", NotificationType.SUCCESS)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = resource.message) }
                        showNotification(resource.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun handleAuthResult(resource: Resource<User>, successMessage: String) {
        when (resource) {
            is Resource.Loading -> setState { copy(isLoading = true, error = null) }
            is Resource.Success -> {
                setState { copy(user = resource.data, isLoading = false, error = null) }
                showNotification(successMessage, NotificationType.SUCCESS)
            }
            is Resource.Error -> {
                setState { copy(isLoading = false, error = resource.message) }
                showNotification(resource.message, NotificationType.ERROR)
            }
        }
    }
}
