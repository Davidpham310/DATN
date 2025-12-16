package com.example.datn.presentation.parent.messaging.viewmodel

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
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel cho Parent SelectRecipient
 * Hiển thị: Children, Teachers, Other Parents (nếu enabled)
 */
@HiltViewModel
class ParentSelectRecipientViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ParentSelectRecipientViewModel.State, ParentSelectRecipientViewModel.Event>(
    initialState = State(),
    notificationManager = notificationManager
) {

    companion object {
        private const val TAG = "ParentSelectRecipientVM"
    }

    data class State(
        override val isLoading: Boolean = false,
        val allRecipients: List<User> = emptyList(),
        val filteredRecipients: List<User> = emptyList(),
        val searchQuery: String = "",
        val selectedRoleFilter: UserRole? = null,
        override val error: String? = null,
        
        // Tab counts for Parent
        val childrenCount: Int = 0,
        val teacherCount: Int = 0,
        val otherParentCount: Int = 0,
        
        // Feature flags
        val showOtherParents: Boolean = true // Can be toggled
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

    private suspend fun awaitNonBlank(flow: Flow<String>): String {
        var result = ""
        flow
            .filter { it.isNotBlank() }
            .take(1)
            .collect { value -> result = value }
        return result
    }

    init {
        loadAllowedRecipients()
    }

    fun loadAllowedRecipients() {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value.ifBlank {
                awaitNonBlank(currentUserIdFlow)
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
                        
                        val children = recipients.count { it.role == UserRole.STUDENT }
                        val teachers = recipients.count { it.role == UserRole.TEACHER }
                        val otherParents = recipients.count { it.role == UserRole.PARENT }
                        
                        Log.d(TAG, "Parent loaded ${recipients.size} allowed recipients (Children:$children, T:$teachers, P:$otherParents)")
                        
                        setState {
                            copy(
                                isLoading = false,
                                allRecipients = recipients,
                                filteredRecipients = applyFilters(recipients, searchQuery, selectedRoleFilter, showOtherParents),
                                childrenCount = children,
                                teacherCount = teachers,
                                otherParentCount = otherParents,
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
            val filtered = applyFilters(allRecipients, query, selectedRoleFilter, showOtherParents)
            copy(
                searchQuery = query,
                filteredRecipients = filtered
            )
        }
    }

    fun filterByRole(role: UserRole?) {
        setState {
            val filtered = applyFilters(allRecipients, searchQuery, role, showOtherParents)
            copy(
                selectedRoleFilter = role,
                filteredRecipients = filtered
            )
        }
    }

    fun toggleOtherParents(show: Boolean) {
        setState {
            copy(
                showOtherParents = show,
                filteredRecipients = applyFilters(allRecipients, searchQuery, selectedRoleFilter, show)
            )
        }
    }

    private fun applyFilters(
        recipients: List<User>,
        query: String,
        roleFilter: UserRole?,
        showOtherParents: Boolean
    ): List<User> {
        var filtered = recipients

        // Apply other parents visibility
        if (!showOtherParents) {
            filtered = filtered.filter { it.role != UserRole.PARENT }
        }

        // Apply role filter
        if (roleFilter != null) {
            filtered = filtered.filter { it.role == roleFilter }
        }

        // Apply search
        if (query.isNotBlank()) {
            val normalizedQuery = normalizeForSearch(query)
            filtered = filtered.filter { user ->
                normalizeForSearch(user.name).contains(normalizedQuery) ||
                normalizeForSearch(user.email).contains(normalizedQuery)
            }
        }

        return filtered
    }

    private fun normalizeForSearch(input: String): String {
        val lower = input.trim().lowercase(Locale.ROOT)
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return normalized
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
    }

    fun selectRecipient(user: User) {
        sendEvent(Event.NavigateToChat(user.id, user.name))
    }

    fun reload() {
        loadAllowedRecipients()
    }
}
