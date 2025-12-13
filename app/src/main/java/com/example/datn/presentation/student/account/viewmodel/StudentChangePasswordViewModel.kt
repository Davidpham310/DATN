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
            showNotification("Vui lòng nhập đầy đủ thông tin", NotificationType.ERROR)
            return
        }

        if (newPassword != confirmPassword) {
            showNotification("Mật khẩu mới và xác nhận mật khẩu không khớp", NotificationType.ERROR)
            return
        }

        val newPasswordResult = passwordValidator.validate(newPassword)
        if (!newPasswordResult.successful) {
            showNotification(newPasswordResult.errorMessage!!, NotificationType.ERROR)
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
                        setState { copy(isLoading = false, error = result.message, successMessage = null) }
                        showNotification(result.message ?: "Đổi mật khẩu thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }
}
