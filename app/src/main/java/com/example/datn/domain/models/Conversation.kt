package com.example.datn.domain.models

import java.time.Instant

data class Conversation(
    val id: String,
    val type: ConversationType,
    val title: String? = null,
    val lastMessageAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)