package com.example.datn.data.local.entities

import androidx.room.Entity
import java.time.Instant

@Entity(
    tableName = "conversation_participant",
    primaryKeys = ["conversationId", "userId"]
)
data class ConversationParticipantEntity(
    val conversationId: String,
    val userId: String,
    val joinedAt: Instant,
    val lastViewedAt: Instant,
    val isMuted: Boolean
)