package com.example.datn.presentation.common.messaging

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.messaging.CreateGroupParams
import com.example.datn.domain.usecase.messaging.MessagingUseCases
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<ConversationListState, ConversationListEvent>(ConversationListState(), notificationManager) {

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    init {
        loadConversations()
    }

    override fun onEvent(event: ConversationListEvent) {
        when (event) {
            is ConversationListEvent.LoadConversations -> loadConversations(event.userId)
            is ConversationListEvent.SelectConversation -> selectConversation(event.conversationId)
            is ConversationListEvent.CreateNewConversation -> createNewConversation(event.recipientId)
            ConversationListEvent.RefreshConversations -> loadConversations()
            is ConversationListEvent.MarkAsRead -> markConversationAsRead(event.conversationId)
            is ConversationListEvent.ToggleMute -> toggleMute(event.conversationId)
            ConversationListEvent.LoadUnreadCount -> loadUnreadCount()
        }
    }

    private fun loadConversations(userId: String? = null) {
        viewModelScope.launch {
            val currentUserId = userId ?: currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            
            if (currentUserId.isBlank()) {
                showNotification("Không xác định được người dùng", NotificationType.ERROR)
                return@launch
            }

            messagingUseCases.getConversations(currentUserId)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                        is Resource.Success -> {
                            val conversations = result.data ?: emptyList()
                            
                            // Log unread count cho mỗi conversation
                            android.util.Log.d("ConversationListVM", "=== LOADED ${conversations.size} CONVERSATIONS ===")
                            conversations.forEach { conv ->
                                android.util.Log.d(
                                    "ConversationListVM",
                                    "Conversation: ${conv.conversationId.take(8)}... | " +
                                    "Type: ${conv.type} | " +
                                    "Title/Name: ${conv.title ?: conv.participantName ?: "Unknown"} | " +
                                    "UnreadCount: ${conv.unreadCount} | " +
                                    "LastMessage: ${conv.lastMessage?.take(30) ?: "None"}"
                                )
                            }
                            android.util.Log.d(
                                "ConversationListVM", 
                                "Total unread messages across all conversations: ${conversations.sumOf { it.unreadCount }}"
                            )
                            
                            setState {
                                copy(
                                    isLoading = false,
                                    conversations = conversations,
                                    error = null
                                )
                            }
                        }
                        is Resource.Error -> {
                            android.util.Log.e("ConversationListVM", "Error loading conversations: ${result.message}")
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
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            
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

    fun createGroupConversation(participantIds: List<String>, groupTitle: String) {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            
            if (currentUserId.isBlank()) {
                showNotification("Không xác định được người dùng", NotificationType.ERROR)
                return@launch
            }

            // Thêm current user vào danh sách participants
            val allParticipants = listOf(currentUserId) + participantIds

            messagingUseCases.createGroupConversation(
                CreateGroupParams(
                    participantIds = allParticipants,
                    groupTitle = groupTitle
                )
            ).onEach { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { 
                            copy(
                                isLoading = false,
                                createdGroupTitle = groupTitle // Lưu tên nhóm để navigate
                            ) 
                        }
                        result.data?.let { conversationId ->
                            selectConversation(conversationId)
                        }
                        showNotification("Tạo nhóm thành công", NotificationType.SUCCESS)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Không thể tạo nhóm", NotificationType.ERROR)
                    }
                }
            }.launchIn(viewModelScope)
        }
    }

    /**
     * Đánh dấu conversation đã đọc
     */
    private fun markConversationAsRead(conversationId: String) {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            
            messagingUseCases.markAsRead(conversationId, currentUserId)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> { /* No UI change needed */ }
                        is Resource.Success -> {
                            // Reload conversations để update unread count
                            loadConversations()
                        }
                        is Resource.Error -> {
                            showNotification(
                                result.message ?: "Không thể đánh dấu đã đọc",
                                NotificationType.ERROR
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * Tắt/bật thông báo cho conversation
     */
    private fun toggleMute(conversationId: String) {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            
            // TODO: Get current mute status from conversation
            val currentlyMuted = false // Placeholder
            
            messagingUseCases.toggleMuteConversation(conversationId, currentUserId, !currentlyMuted)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> { /* No UI change */ }
                        is Resource.Success -> {
                            showNotification(
                                if (currentlyMuted) "Đã bật thông báo" else "Đã tắt thông báo",
                                NotificationType.SUCCESS
                            )
                            loadConversations() // Reload to update UI
                        }
                        is Resource.Error -> {
                            showNotification(
                                result.message ?: "Không thể thay đổi trạng thái thông báo",
                                NotificationType.ERROR
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * Tải tổng số tin nhắn chưa đọc
     */
    private fun loadUnreadCount() {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            
            try {
                val result = messagingUseCases.getUnreadCount.getTotalUnreadCount(currentUserId)
                when (result) {
                    is Resource.Success -> {
                        setState { copy(totalUnreadCount = result.data ?: 0) }
                    }
                    is Resource.Error -> {
                        // Silent fail for unread count
                        setState { copy(totalUnreadCount = 0) }
                    }
                    is Resource.Loading -> { /* No action */ }
                }
            } catch (e: Exception) {
                // Silent fail
                setState { copy(totalUnreadCount = 0) }
            }
        }
    }

    /**
     * Refresh conversations (pull to refresh)
     */
    fun refresh() {
        loadConversations()
        loadUnreadCount()
    }
}
