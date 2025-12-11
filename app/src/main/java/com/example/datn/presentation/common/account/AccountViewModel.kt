package com.example.datn.presentation.common.account

import android.util.Log
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.user.GetUserByIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    private val getUserById: GetUserByIdUseCase,
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
        Log.d("AccountViewModel", "🔹 START loadCurrentUser")
        launch {
            authUseCases.getCurrentUser().collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d("AccountViewModel", "⏳ Loading current user...")
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val user = result.data
                        Log.d("AccountViewModel", "✅ Loaded current user: ${user?.name}")
                        Log.d("AccountViewModel", "📧 Email: ${user?.email}")
                        Log.d("AccountViewModel", "🖼️ Avatar URL from auth: ${user?.avatarUrl}")
                        Log.d("AccountViewModel", "👤 User ID: ${user?.id}")
                        setState {
                            copy(
                                isLoading = false,
                                currentUser = result.data,
                                error = null
                            )
                        }
                        // Load full user data with avatar URL
                        user?.id?.let { userId ->
                            loadUserWithAvatar(userId)
                        }
                    }
                    is Resource.Error -> {
                        Log.e("AccountViewModel", "❌ Error loading user: ${result.message}")
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message, NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun loadUserWithAvatar(userId: String) {
        Log.d("AccountViewModel", "🔹 Loading full user data with avatar for userId: $userId")
        launch {
            getUserById(userId).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d("AccountViewModel", "⏳ Loading user with avatar...")
                    }
                    is Resource.Success -> {
                        val userWithAvatar = result.data
                        Log.d("AccountViewModel", "✅ Loaded user with avatar: ${userWithAvatar?.name}")
                        Log.d("AccountViewModel", "🖼️ Avatar URL from getUserById: ${userWithAvatar?.avatarUrl}")
                        // Update state with user that has avatar URL
                        setState { copy(currentUser = userWithAvatar) }
                    }
                    is Resource.Error -> {
                        Log.w("AccountViewModel", "⚠️ Failed to load user with avatar: ${result.message}")
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
                        dismissNotifications(clearQueue = true)
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
