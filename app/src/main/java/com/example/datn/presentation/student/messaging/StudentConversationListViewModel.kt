package com.example.datn.presentation.student.messaging

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.messaging.MessagingUseCases
import com.example.datn.presentation.common.messaging.ConversationListEvent
import com.example.datn.presentation.common.messaging.ConversationListState
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentConversationListViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ConversationListState, ConversationListEvent>(
    ConversationListState(),
    notificationManager
) {

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    init {
        loadConversations()
    }

    override fun onEvent(event: ConversationListEvent) {
        when (event) {
            is ConversationListEvent.LoadConversations -> loadConversations(event.userId)
            is ConversationListEvent.SelectConversation -> setState { 
                copy(selectedConversationId = event.conversationId)
            }
            is ConversationListEvent.CreateNewConversation -> createNewConversation(event.recipientId)
            ConversationListEvent.RefreshConversations -> loadConversations()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadConversations(userId: String? = null) {
        if (userId != null) {
            // Load specific user conversations
            loadConversationsForUser(userId)
        } else {
            // Auto-load for current user with reactive flow
            viewModelScope.launch {
                currentUserIdFlow
                    .filter { it.isNotBlank() }
                    .flatMapLatest { currentUserId ->
                        messagingUseCases.getConversations(currentUserId)
                    }
                    .collect { result ->
                        when (result) {
                            is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                            is Resource.Success -> setState {
                                copy(
                                    isLoading = false,
                                    conversations = result.data ?: emptyList(),
                                    error = null
                                )
                            }
                            is Resource.Error -> {
                                setState { copy(isLoading = false, error = result.message) }
                                showNotification(
                                    result.message ?: "Không thể tải danh sách hội thoại",
                                    NotificationType.ERROR
                                )
                            }
                        }
                    }
            }
        }
    }

    private fun loadConversationsForUser(userId: String) {
        messagingUseCases.getConversations(userId)
            .onEach { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> setState {
                        copy(
                            isLoading = false,
                            conversations = result.data ?: emptyList(),
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(
                            result.message ?: "Không thể tải danh sách hội thoại",
                            NotificationType.ERROR
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun createNewConversation(recipientId: String) {
        viewModelScope.launch {
            // Đợi userId hợp lệ từ StateFlow
            val currentUserId = currentUserIdFlow
                .filter { it.isNotBlank() }
                .first()

            messagingUseCases.createConversation(currentUserId, recipientId)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { copy(isLoading = false) }
                            result.data?.let { conversation ->
                                setState { copy(selectedConversationId = conversation.id) }
                            }
                            showNotification("Tạo hội thoại thành công", NotificationType.SUCCESS)
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            showNotification(
                                result.message ?: "Không thể tạo hội thoại",
                                NotificationType.ERROR
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }
}
