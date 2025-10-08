package com.example.datn.domain.models

import java.time.Instant

data class ConversationParticipant(
    val conversationId: String,
    val userId: String,
    val joinedAt: Instant,
    val lastViewedAt: Instant,
    val isMuted: Boolean
)
