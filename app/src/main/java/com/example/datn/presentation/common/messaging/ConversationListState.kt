package com.example.datn.presentation.common.messaging

import com.example.datn.core.base.BaseState
import com.example.datn.data.local.dao.ConversationWithListDetails

data class ConversationListState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val conversations: List<ConversationWithListDetails> = emptyList(),
    val selectedConversationId: String? = null,
    val createdGroupTitle: String? = null, // Tên nhóm vừa tạo để navigate
    val totalUnreadCount: Int = 0 // Total unread messages across all conversations
) : BaseState
