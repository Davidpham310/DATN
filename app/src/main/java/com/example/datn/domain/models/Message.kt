package com.example.datn.domain.models

import java.time.Instant

data class Message(
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