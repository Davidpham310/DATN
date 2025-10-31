package com.example.datn.presentation.teacher.messaging

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    private val notificationManager: NotificationManager
) : BaseViewModel<ConversationListState, ConversationListEvent>(ConversationListState(), notificationManager) {

    init {
        loadConversations()
    }

    override fun onEvent(event: ConversationListEvent) {
        when (event) {
            is ConversationListEvent.LoadConversations -> loadConversations(event.userId)
            is ConversationListEvent.SelectConversation -> selectConversation(event.conversationId)
            is ConversationListEvent.CreateNewConversation -> createNewConversation(event.recipientId)
            ConversationListEvent.RefreshConversations -> loadConversations()
        }
    }

    private fun loadConversations(userId: String? = null) {
        viewModelScope.launch {
            val currentUserId = userId ?: authUseCases.getCurrentIdUser.invoke().first()
            if (currentUserId.isBlank()) {
                showNotification("Không xác định được người dùng", NotificationType.ERROR)
                return@launch
            }

            messagingUseCases.getConversations(currentUserId)
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
                            showNotification(result.message ?: "Không thể tải danh sách hội thoại", NotificationType.ERROR)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun selectConversation(conversationId: String) {
        setState { copy(selectedConversationId = conversationId) }
    }

    private fun createNewConversation(recipientId: String) {
        viewModelScope.launch {
            val currentUserId = authUseCases.getCurrentIdUser.invoke().first()
            if (currentUserId.isBlank()) {
                showNotification("Không xác định được người dùng", NotificationType.ERROR)
                return@launch
            }

            messagingUseCases.createConversation(currentUserId, recipientId)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { copy(isLoading = false) }
                            result.data?.let { conversation ->
                                selectConversation(conversation.id)
                            }
                            showNotification("Tạo hội thoại thành công", NotificationType.SUCCESS)
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            showNotification(result.message ?: "Không thể tạo hội thoại", NotificationType.ERROR)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }
}
