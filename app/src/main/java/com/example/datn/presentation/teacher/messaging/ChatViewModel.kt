package com.example.datn.presentation.teacher.messaging

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.messaging.MessagingUseCases
import com.example.datn.domain.usecase.messaging.SendMessageParams
import com.example.datn.presentation.common.messaging.ChatEvent
import com.example.datn.presentation.common.messaging.ChatState
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    private val notificationManager: NotificationManager
) : BaseViewModel<ChatState, ChatEvent>(ChatState(), notificationManager) {

    override fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.LoadConversation -> loadConversation(event.conversationId, event.recipientId, event.recipientName)
            is ChatEvent.SendMessage -> sendMessage(event.content)
            is ChatEvent.UpdateMessageInput -> setState { copy(messageInput = event.text) }
            ChatEvent.MarkAsRead -> markAsRead()
        }
    }

    private fun loadConversation(conversationId: String, recipientId: String, recipientName: String) {
        setState {
            copy(
                conversationId = conversationId,
                recipientId = recipientId,
                recipientName = recipientName
            )
        }

        // Load messages
        messagingUseCases.getMessages(conversationId)
            .onEach { message ->
                val currentMessages = state.value.messages.toMutableList()
                if (!currentMessages.any { it.id == message.id }) {
                    currentMessages.add(message)
                    currentMessages.sortBy { it.sentAt }
                    setState { copy(messages = currentMessages) }
                }
            }
            .launchIn(viewModelScope)

        // Mark as read
        markAsRead()
    }

    private fun sendMessage(content: String) {
        if (content.isBlank()) {
            showNotification("Vui lòng nhập nội dung tin nhắn", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val currentUserId = authUseCases.getCurrentIdUser.invoke().first()
            if (currentUserId.isBlank()) {
                showNotification("Không xác định được người dùng", NotificationType.ERROR)
                return@launch
            }

            val recipientId = state.value.recipientId
            if (recipientId.isBlank()) {
                showNotification("Không xác định được người nhận", NotificationType.ERROR)
                return@launch
            }

            setState { copy(isSending = true) }

            messagingUseCases.sendMessage(
                SendMessageParams(
                    senderId = currentUserId,
                    recipientId = recipientId,
                    content = content
                )
            ).onEach { result ->
                when (result) {
                    is Resource.Loading -> { /* Already handled */ }
                    is Resource.Success -> {
                        setState { copy(isSending = false, messageInput = "") }
                    }
                    is Resource.Error -> {
                        setState { copy(isSending = false) }
                        showNotification(result.message ?: "Không thể gửi tin nhắn", NotificationType.ERROR)
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            val currentUserId = authUseCases.getCurrentIdUser.invoke().first()
            val conversationId = state.value.conversationId

            if (currentUserId.isNotBlank() && conversationId.isNotBlank()) {
                messagingUseCases.markAsRead(conversationId, currentUserId)
                    .launchIn(viewModelScope)
            }
        }
    }
}
