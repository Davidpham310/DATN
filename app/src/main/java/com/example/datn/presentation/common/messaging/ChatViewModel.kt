package com.example.datn.presentation.common.messaging

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ConversationType
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.messaging.MessagingUseCases
import com.example.datn.domain.usecase.messaging.SendMessageParams
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
class ChatViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    private val userDao: com.example.datn.data.local.dao.UserDao,
    notificationManager: NotificationManager
) : BaseViewModel<ChatState, ChatEvent>(ChatState(), notificationManager) {

    private var messageListenerJob: Job? = null
    private var markAsReadJob: Job? = null

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

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
            // Lấy userId từ StateFlow cached
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            
            // Xác định conversation type: nếu recipientId rỗng thì là GROUP, ngược lại là ONE_TO_ONE
            val conversationType = if (recipientId.isBlank()) {
                ConversationType.GROUP
            } else {
                ConversationType.ONE_TO_ONE
            }
            
            Log.d("ChatViewModel", "loadConversation - conversationId: $conversationId, recipientId: '$recipientId', recipientName: $recipientName, type: $conversationType")
            
            setState {
                copy(
                    conversationId = conversationId,
                    recipientId = recipientId,
                    recipientName = recipientName,
                    currentUserId = currentUserId,
                    conversationType = conversationType
                )
            }

            if (conversationId != "new") {
                Log.d("ChatViewModel", "Starting message listener for conversation: $conversationId")
                startMessageListener(conversationId)
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
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }

            // Lấy recipientId từ state hiện tại
            val currentState = state.value
            val recipientId = currentState.recipientId
            
            // Debug log
            Log.d("ChatViewModel", "SendMessage - conversationId: ${currentState.conversationId}, recipientId: $recipientId, recipientName: ${currentState.recipientName}")
            
            // Chỉ check recipientId khi tạo conversation mới (1-1 chat)
            if (currentState.conversationId == "new" && recipientId.isBlank()) {
                showNotification("Không xác định được người nhận. Vui lòng chọn lại.", NotificationType.ERROR)
                return@launch
            }

            setState { copy(isSending = true) }

            // Nếu đã có conversationId (không phải "new"), truyền conversationId để gửi vào conversation có sẵn
            val targetConversationId = if (currentState.conversationId != "new") {
                currentState.conversationId
            } else {
                null
            }

            messagingUseCases.sendMessage(
                SendMessageParams(
                    senderId = currentUserId,
                    recipientId = recipientId,
                    content = content,
                    conversationId = targetConversationId
                )
            ).onEach { result ->
                when (result) {
                    is Resource.Loading -> { /* Already handled */ }
                    is Resource.Success -> {
                        setState { copy(isSending = false, messageInput = "") }
                        
                        if (state.value.conversationId == "new") {
                            findAndLoadConversation(currentUserId, recipientId)
                        } else {
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
            kotlinx.coroutines.delay(300)
            
            try {
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
                    setState { 
                        copy(
                            conversationId = foundConversation!!.conversationId
                        ) 
                    }
                    
                    startMessageListener(foundConversation!!.conversationId)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error finding conversation: ${e.message}")
            }
        }
    }

    private fun startMessageListener(conversationId: String) {
        // Cancel existing listener để tránh multiple listeners
        messageListenerJob?.cancel()
        
        Log.d("ChatViewModel", "🎧 Starting message listener for: $conversationId")
        
        // Chỉ clear messages nếu CHUYỂN conversation khác
        if (state.value.conversationId != conversationId) {
            Log.d("ChatViewModel", "🔄 Different conversation, clearing messages")
            setState { copy(messages = emptyList()) }
        } else {
            Log.d("ChatViewModel", "♻️ Same conversation, keeping messages")
        }
        
        messageListenerJob = viewModelScope.launch {
            messagingUseCases.getMessages(conversationId)
                .onEach { message ->
                    val currentMessages = state.value.messages.toMutableList()
                    val exists = currentMessages.any { it.id == message.id }
                    
                    Log.d("ChatViewModel", "📩 Received message: ${message.id.take(8)}... | Exists in list: $exists | Current total: ${currentMessages.size}")
                    
                    if (!exists) {
                        currentMessages.add(message)
                        currentMessages.sortBy { it.sentAt }
                        setState { copy(messages = currentMessages) }
                        
                        Log.d("ChatViewModel", "✅ Message added: ${message.id.take(8)}... | New total: ${currentMessages.size} | Sender: ${message.senderId.take(8)}...")
                        
                        // Fetch sender name nếu là group chat và chưa có trong map
                        if (state.value.conversationType == ConversationType.GROUP && 
                            !state.value.senderNames.containsKey(message.senderId)) {
                            fetchSenderName(message.senderId)
                        }
                        
                        // Tự động đánh dấu đã đọc khi nhận tin nhắn mới
                        markAsRead()
                    } else {
                        Log.d("ChatViewModel", "⏭️ Duplicate message skipped: ${message.id.take(8)}...")
                    }
                }
                .launchIn(this)
        }
    }
    
    private fun reloadMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ChatViewModel", "Reloading messages for conversation: $conversationId")
                kotlinx.coroutines.delay(200)
                startMessageListener(conversationId)
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error reloading messages: ${e.message}")
            }
        }
    }
    
    private fun markAsRead() {
        // Cancel previous job để debounce - tránh gọi quá nhiều lần
        markAsReadJob?.cancel()
        
        markAsReadJob = viewModelScope.launch {
            // Delay 500ms để gom nhiều calls thành 1
            kotlinx.coroutines.delay(500)
            
            val currentUserId = currentUserIdFlow.value
            val conversationId = state.value.conversationId

            if (currentUserId.isNotBlank() && conversationId.isNotBlank() && conversationId != "new") {
                Log.d("ChatViewModel", "Marking conversation as read: $conversationId for user: $currentUserId")
                messagingUseCases.markAsRead(conversationId, currentUserId)
                    .onEach { result ->
                        when (result) {
                            is Resource.Success -> {
                                Log.d("ChatViewModel", "Successfully marked as read: $conversationId")
                            }
                            is Resource.Error -> {
                                Log.e("ChatViewModel", "Failed to mark as read: ${result.message}")
                            }
                            else -> {}
                        }
                    }
                    .launchIn(viewModelScope)
            }
        }
    }
    
    private fun fetchSenderName(senderId: String) {
        viewModelScope.launch {
            try {
                val user = userDao.getUserById(senderId)
                if (user != null) {
                    val currentMap = state.value.senderNames.toMutableMap()
                    currentMap[senderId] = user.name
                    setState { copy(senderNames = currentMap) }
                    Log.d("ChatViewModel", "Fetched sender name: ${user.name} for $senderId")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching sender name: ${e.message}")
            }
        }
    }
}
