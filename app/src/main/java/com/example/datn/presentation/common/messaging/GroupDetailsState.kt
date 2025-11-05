package com.example.datn.presentation.common.messaging

import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.User

data class GroupDetailsState(
    val participants: List<User> = emptyList(),
    val currentUserId: String = "",
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val hasLeftGroup: Boolean = false
) : BaseState

sealed class GroupDetailsEvent : BaseEvent {
    data class LoadParticipants(val conversationId: String) : GroupDetailsEvent()
    data class LeaveGroup(val conversationId: String) : GroupDetailsEvent()
}
