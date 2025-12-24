package com.example.datn.presentation.student.account.viewmodel

import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.rules.auth.PasswordValidator
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.auth.ChangePasswordParams
import com.example.datn.presentation.common.auth.ChangePasswordEvent
import com.example.datn.presentation.common.auth.ChangePasswordState
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@HiltViewModel
class StudentChangePasswordViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ChangePasswordState, ChangePasswordEvent>(
    ChangePasswordState(),
    notificationManager
) {

    private val passwordValidator = PasswordValidator()

    override fun onEvent(event: ChangePasswordEvent) {
        when (event) {
            is ChangePasswordEvent.OnChangePassword -> changePassword(
                event.currentPassword,
                event.newPassword,
                event.confirmPassword
            )
            is ChangePasswordEvent.ClearMessages -> setState { copy(error = null, successMessage = null) }
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            val message = "Vui lòng nhập đầy đủ thông tin"
            setState { copy(error = message, successMessage = null) }
            showNotification(message, NotificationType.ERROR)
            return
        }

        if (newPassword != confirmPassword) {
            val message = "Mật khẩu xác nhận không khớp"
            setState { copy(error = message, successMessage = null) }
            showNotification(message, NotificationType.ERROR)
            return
        }

        val newPasswordResult = passwordValidator.validate(newPassword)
        if (!newPasswordResult.successful) {
            val message = newPasswordResult.errorMessage ?: "Mật khẩu mới không hợp lệ"
            setState { copy(error = message, successMessage = null) }
            showNotification(message, NotificationType.ERROR)
            return
        }

        launch {
            authUseCases.changePassword(ChangePasswordParams(currentPassword, newPassword)).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null, successMessage = null) }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                successMessage = "Đổi mật khẩu thành công"
                            )
                        }
                        showNotification("Đổi mật khẩu thành công", NotificationType.SUCCESS)
                    }
                    is Resource.Error -> {
                        val rawMessage = result.message
                        val message = when {
                            rawMessage.isNullOrBlank() -> "Đổi mật khẩu thất bại"
                            rawMessage == "Sai mật khẩu, vui lòng thử lại." -> "Mật khẩu hiện tại không chính xác"
                            rawMessage == "Email hoặc mật khẩu không hợp lệ." -> "Mật khẩu hiện tại không chính xác"
                            rawMessage.contains("WRONG_PASSWORD", ignoreCase = true) -> "Mật khẩu hiện tại không chính xác"
                            rawMessage.contains("INVALID_CREDENTIAL", ignoreCase = true) -> "Mật khẩu hiện tại không chính xác"
                            rawMessage.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) -> "Mật khẩu hiện tại không chính xác"
                            else -> rawMessage
                        }

                        setState { copy(isLoading = false, error = message, successMessage = null) }
                        showNotification(message, NotificationType.ERROR)
                    }
                }
            }
        }
    }
}
