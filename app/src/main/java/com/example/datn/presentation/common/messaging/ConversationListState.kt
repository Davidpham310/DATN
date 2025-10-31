package com.example.datn.presentation.common.messaging

import com.example.datn.core.base.BaseState
import com.example.datn.data.local.dao.ConversationWithListDetails

data class ConversationListState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val conversations: List<ConversationWithListDetails> = emptyList(),
    val selectedConversationId: String? = null
) : BaseState
