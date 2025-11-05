package com.example.datn.presentation.common.messaging

import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseEvent

data class AddMembersState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isSuccess: Boolean = false
) : BaseState

sealed class AddMembersEvent : BaseEvent {
    data class AddMembers(val conversationId: String, val userIds: List<String>) : AddMembersEvent()
}
