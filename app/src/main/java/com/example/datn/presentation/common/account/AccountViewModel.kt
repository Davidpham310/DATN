package com.example.datn.presentation.common.account

import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<AccountState, AccountEvent>(AccountState(), notificationManager) {

    init {
        loadCurrentUser()
    }

    override fun onEvent(event: AccountEvent) {
        when (event) {
            is AccountEvent.LoadCurrentUser -> loadCurrentUser()
            is AccountEvent.SignOut -> signOut()
            is AccountEvent.ClearMessages -> setState { copy(successMessage = null, error = null) }
        }
    }

    private fun loadCurrentUser() {
        launch {
            authUseCases.getCurrentUser().collectLatest { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                currentUser = result.data,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun signOut() {
        launch {
            authUseCases.signOut().collectLatest { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isSigningOut = true) }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isSigningOut = false,
                                currentUser = null,
                                successMessage = "Đăng xuất thành công"
                            )
                        }
                        showNotification("Đăng xuất thành công", NotificationType.SUCCESS)
                    }
                    is Resource.Error -> {
                        setState { copy(isSigningOut = false, error = result.message) }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }
}
