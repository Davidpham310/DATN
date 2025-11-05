package com.example.datn.presentation.student.messaging

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseEvent
import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.messaging.MessagingUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho Student SelectRecipient
 * Hiển thị: Teachers, Parents, Classmates (nếu enabled)
 */
@HiltViewModel
class StudentSelectRecipientViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<StudentSelectRecipientViewModel.State, StudentSelectRecipientViewModel.Event>(
    initialState = State(),
    notificationManager = notificationManager
) {

    companion object {
        private const val TAG = "StudentSelectRecipientVM"
    }

    data class State(
        override val isLoading: Boolean = false,
        val allRecipients: List<User> = emptyList(),
        val filteredRecipients: List<User> = emptyList(),
        val searchQuery: String = "",
        val selectedRoleFilter: UserRole? = null,
        override val error: String? = null,
        
        // Tab counts for Student
        val teacherCount: Int = 0,
        val parentCount: Int = 0,
        val classmateCount: Int = 0,
        
        // Feature flags
        val showClassmates: Boolean = true // Can be toggled
    ) : BaseState

    sealed class Event : BaseEvent {
        data class ShowError(val message: String) : Event()
        data class NavigateToChat(val userId: String, val userName: String) : Event()
    }

    override fun onEvent(event: Event) {
        // Handle events if needed
    }

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    init {
        loadAllowedRecipients()
    }

    fun loadAllowedRecipients() {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }

            if (currentUserId.isBlank()) {
                Log.e(TAG, "Current user ID is blank")
                setState { copy(error = "Không xác định được người dùng") }
                return@launch
            }

            messagingUseCases.getAllowedRecipients(currentUserId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        val recipients = resource.data ?: emptyList()
                        
                        val teachers = recipients.count { it.role == UserRole.TEACHER }
                        val parents = recipients.count { it.role == UserRole.PARENT }
                        val classmates = recipients.count { it.role == UserRole.STUDENT }
                        
                        Log.d(TAG, "Student loaded ${recipients.size} allowed recipients (T:$teachers, P:$parents, C:$classmates)")
                        
                        setState {
                            copy(
                                isLoading = false,
                                allRecipients = recipients,
                                filteredRecipients = applyFilters(recipients, searchQuery, selectedRoleFilter),
                                teacherCount = teachers,
                                parentCount = parents,
                                classmateCount = classmates,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error loading recipients: ${resource.message}")
                        setState {
                            copy(
                                isLoading = false,
                                error = resource.message
                            )
                        }
                        sendEvent(Event.ShowError(resource.message ?: "Lỗi tải danh sách"))
                    }
                }
            }
        }
    }

    fun search(query: String) {
        setState {
            val filtered = applyFilters(allRecipients, query, selectedRoleFilter)
            copy(
                searchQuery = query,
                filteredRecipients = filtered
            )
        }
    }

    fun filterByRole(role: UserRole?) {
        setState {
            val filtered = applyFilters(allRecipients, searchQuery, role)
            copy(
                selectedRoleFilter = role,
                filteredRecipients = filtered
            )
        }
    }

    fun toggleClassmates(show: Boolean) {
        setState {
            val filtered = if (show) {
                allRecipients
            } else {
                allRecipients.filter { it.role != UserRole.STUDENT }
            }
            copy(
                showClassmates = show,
                filteredRecipients = applyFilters(filtered, searchQuery, selectedRoleFilter)
            )
        }
    }

    private fun applyFilters(recipients: List<User>, query: String, roleFilter: UserRole?): List<User> {
        var filtered = recipients

        // Apply classmate visibility
        if (!state.value.showClassmates) {
            filtered = filtered.filter { it.role != UserRole.STUDENT }
        }

        // Apply role filter
        if (roleFilter != null) {
            filtered = filtered.filter { it.role == roleFilter }
        }

        // Apply search
        if (query.isNotBlank()) {
            filtered = filtered.filter { user ->
                user.name.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
        }

        return filtered
    }

    fun selectRecipient(user: User) {
        sendEvent(Event.NavigateToChat(user.id, user.name))
    }

    fun reload() {
        loadAllowedRecipients()
    }
}
