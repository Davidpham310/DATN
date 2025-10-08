package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.ConversationType
import java.time.Instant

@Entity(tableName = "conversation")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val type: ConversationType,
    val title: String? = null,
    val lastMessageAt: Instant,
    val createdAt: Instant,
    val updatedAt:  Instant
)