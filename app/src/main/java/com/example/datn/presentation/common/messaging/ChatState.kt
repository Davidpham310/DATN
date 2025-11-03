package com.example.datn.presentation.common.messaging

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Message

data class ChatState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val conversationId: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val currentUserId: String = "",
    val messages: List<Message> = emptyList(),
    val messageInput: String = "",
    val isSending: Boolean = false
) : BaseState
