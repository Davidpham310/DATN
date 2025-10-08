package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "message")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val sentAt: Instant,
    val isRead: Boolean,
    val conversationId: String,
    val createdAt: Instant,
    val updatedAt: Instant
)