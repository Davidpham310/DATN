package com.example.datn.presentation.common.messaging

import com.example.datn.core.base.BaseEvent

sealed class ChatEvent : BaseEvent {
    data class LoadConversation(val conversationId: String, val recipientId: String, val recipientName: String) : ChatEvent()
    data class SendMessage(val content: String) : ChatEvent()
    data class UpdateMessageInput(val text: String) : ChatEvent()
    object MarkAsRead : ChatEvent()
}
