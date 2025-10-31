package com.example.datn.presentation.common.messaging

import com.example.datn.core.base.BaseEvent

sealed class ConversationListEvent : BaseEvent {
    data class LoadConversations(val userId: String) : ConversationListEvent()
    data class SelectConversation(val conversationId: String) : ConversationListEvent()
    data class CreateNewConversation(val recipientId: String) : ConversationListEvent()
    object RefreshConversations : ConversationListEvent()
}
