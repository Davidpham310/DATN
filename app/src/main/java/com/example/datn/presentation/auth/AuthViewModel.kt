package com.example.datn.presentation.auth

import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.rules.EmailValidator
import com.example.datn.core.utils.validation.rules.PasswordValidator
import com.example.datn.core.utils.validation.rules.UsernameValidator
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.auth.ForgotPasswordParams
import com.example.datn.domain.usecase.auth.LoginParams
import com.example.datn.domain.usecase.auth.RegisterParams
import com.example.datn.presentation.common.AuthEvent
import com.example.datn.presentation.common.AuthState
import com.example.datn.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : BaseViewModel<AuthState, AuthEvent>(AuthState()) {

    private val emailValidator = EmailValidator()
    private val passwordValidator = PasswordValidator()
    private val usernameValidator = UsernameValidator()
    override fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.OnLogin -> login(event.email, event.password)
            is AuthEvent.OnRegister -> register(event.email, event.password, event.name, event.role)
            is AuthEvent.OnForgotPassword -> forgotPassword(event.email)
        }
    }

    private fun login(email: String, password: String) {
        // Validation
        val emailResult = emailValidator.validate(email)
        val passwordResult = passwordValidator.validate(password)

        if (!emailResult.successful) {
            showNotification(emailResult.errorMessage!!, NotificationType.ERROR)
            return
        }
        if (!passwordResult.successful) {
            showNotification(passwordResult.errorMessage!!, NotificationType.ERROR)
            return
        }

        launch {
            authUseCases.login(LoginParams(email, password)).collect { resource ->
                handleAuthResult(resource, successMessage = "Login successful")
            }
        }
    }

    private fun register(email: String, password: String, name: String, role: String) {

        val emailResult = emailValidator.validate(email)
        val passwordResult = passwordValidator.validate(password)
        val usernameResult = usernameValidator.validate(name)

        if (!emailResult.successful) {
            showNotification(emailResult.errorMessage!!, NotificationType.ERROR)
            return
        }
        if (!passwordResult.successful) {
            showNotification(passwordResult.errorMessage!!, NotificationType.ERROR)
            return
        }
        if (!usernameResult.successful) {
            showNotification(usernameResult.errorMessage!!, NotificationType.ERROR)
            return
        }

        launch {
            authUseCases.register(RegisterParams(email, password, name, role)).collect { resource ->
                handleAuthResult(resource, successMessage = "Register successful")
            }
        }
    }

    private fun forgotPassword(email: String) {
        val emailResult = emailValidator.validate(email)
        if (!emailResult.successful) {
            showNotification(emailResult.errorMessage!!, NotificationType.ERROR)
            return
        }

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

    fun clearNavigation() {
        setState { copy(navigateTo = null) }
    }

    private fun handleAuthResult(resource: Resource<User>, successMessage: String) {
        when (resource) {
            is Resource.Loading -> setState { copy(isLoading = true, error = null) }
            is Resource.Success -> {
                setState {
                    copy(
                        user = resource.data,
                        isLoading = false,
                        error = null,
                        navigateTo = when (resource.data.role) {
                            UserRole.TEACHER  -> Screen.TeacherHome.route
                            UserRole.PARENT  -> Screen.ParentHome.route
                            UserRole.STUDENT  -> Screen.StudentHome.route
                        }
                    )
                }
                showNotification(successMessage, NotificationType.SUCCESS)
            }
            is Resource.Error -> {
                setState { copy(isLoading = false, error = resource.message) }
                showNotification(resource.message, NotificationType.ERROR)
            }
        }
    }
}
