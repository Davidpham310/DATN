package com.example.datn.presentation.parent.messaging

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentChatViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ChatState, ChatEvent>(ChatState(), notificationManager) {

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )
    
    // Job để quản lý message listener
    private var messageListenerJob: kotlinx.coroutines.Job? = null

    override fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.LoadConversation -> loadConversation(event.conversationId, event.recipientId, event.recipientName)
            is ChatEvent.SendMessage -> sendMessage(event.content)
            is ChatEvent.UpdateMessageInput -> setState { copy(messageInput = event.text) }
            ChatEvent.MarkAsRead -> markAsRead()
        }
    }

    private fun loadConversation(conversationId: String, recipientId: String, recipientName: String) {
        viewModelScope.launch {
            // Debug log
            android.util.Log.d("ParentChatVM", "LoadConversation - conversationId: $conversationId, recipientId: $recipientId, recipientName: $recipientName")
            
            // Lấy userId từ StateFlow cached
            val currentUserId = currentUserIdFlow.value.ifBlank {
                // Nếu chưa có, đợi giá trị hợp lệ
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            
            setState {
                copy(
                    conversationId = conversationId,
                    recipientId = recipientId,
                    recipientName = recipientName,
                    currentUserId = currentUserId
                )
            }
            
            // Verify state after set
            android.util.Log.d("ParentChatVM", "State after set - conversationId: ${state.value.conversationId}, recipientId: ${state.value.recipientId}")

            // Nếu là conversation mới (conversationId = "new"), không load messages
            // Messages sẽ được load sau khi gửi tin nhắn đầu tiên
            if (conversationId != "new") {
                // Start message listener
                startMessageListener(conversationId)

                // Mark as read
                markAsRead()
            }
        }
    }

    private fun sendMessage(content: String) {
        if (content.isBlank()) {
            showNotification("Vui lòng nhập nội dung tin nhắn", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            // Lấy userId từ StateFlow cached
            val currentUserId = currentUserIdFlow.value.ifBlank {
                // Nếu chưa có, đợi giá trị hợp lệ
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }

            // Lấy recipientId từ state hiện tại
            val currentState = state.value
            val recipientId = currentState.recipientId
            
            // Debug log
            android.util.Log.d("ParentChatVM", "SendMessage - conversationId: ${currentState.conversationId}, recipientId: $recipientId, recipientName: ${currentState.recipientName}")
            
            if (recipientId.isBlank()) {
                showNotification("Không xác định được người nhận. Vui lòng chọn lại giáo viên.", NotificationType.ERROR)
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
                        
                        // Nếu là conversation mới, cần load messages sau khi gửi thành công
                        if (state.value.conversationId == "new") {
                            // Tìm conversation vừa tạo bằng cách query từ DAO
                            findAndLoadConversation(currentUserId, recipientId)
                        } else {
                            // Conversation đã tồn tại, reload messages để hiển thị message mới
                            reloadMessages(state.value.conversationId)
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isSending = false) }
                        showNotification(result.message ?: "Không thể gửi tin nhắn", NotificationType.ERROR)
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
    
    private fun findAndLoadConversation(userId: String, recipientId: String) {
        viewModelScope.launch {
            // Sau khi gửi message, repository đã tạo conversation
            // Đợi một chút để đảm bảo conversation đã được tạo
            kotlinx.coroutines.delay(300)
            
            try {
                // Load lại conversations và tìm conversation với recipientId
                // Dùng take(1) thay vì first() để tránh Flow transparency violation
                var foundConversation: com.example.datn.data.local.dao.ConversationWithListDetails? = null
                
                messagingUseCases.getConversations(userId)
                    .take(1)
                    .collect { resource ->
                        if (resource is Resource.Success) {
                            foundConversation = resource.data?.find { 
                                it.participantUserId == recipientId 
                            }
                        }
                    }
                
                if (foundConversation != null) {
                    // Update conversationId và bắt đầu load messages
                    // Giữ nguyên recipientId và recipientName
                    setState { 
                        copy(
                            conversationId = foundConversation!!.conversationId
                            // recipientId và recipientName đã có từ loadConversation
                        ) 
                    }
                    
                    // Start message listener với conversationId mới
                    startMessageListener(foundConversation!!.conversationId)
                }
            } catch (e: Exception) {
                android.util.Log.e("ParentChatVM", "Error finding conversation: ${e.message}")
            }
        }
    }

    private fun startMessageListener(conversationId: String) {
        // Cancel listener cũ nếu có
        messageListenerJob?.cancel()
        
        // Start listener mới
        messageListenerJob = viewModelScope.launch {
            messagingUseCases.getMessages(conversationId)
                .onEach { message ->
                    val currentMessages = state.value.messages.toMutableList()
                    if (!currentMessages.any { it.id == message.id }) {
                        currentMessages.add(message)
                        currentMessages.sortBy { it.sentAt }
                        setState { copy(messages = currentMessages) }
                    }
                }
                .launchIn(this)
        }
    }
    
    private fun reloadMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ParentChatVM", "Reloading messages for conversation: $conversationId")
                
                // Delay ngắn để đảm bảo message đã được insert vào DB
                kotlinx.coroutines.delay(200)
                
                // Cancel và restart message listener để force reload
                startMessageListener(conversationId)
                
            } catch (e: Exception) {
                android.util.Log.e("ParentChatVM", "Error reloading messages: ${e.message}")
            }
        }
    }
    
    private fun markAsRead() {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value
            val conversationId = state.value.conversationId

            if (currentUserId.isNotBlank() && conversationId.isNotBlank() && conversationId != "new") {
                messagingUseCases.markAsRead(conversationId, currentUserId)
                    .launchIn(viewModelScope)
            }
        }
    }
}
