package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ConversationEntity
import com.example.datn.domain.models.Conversation

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    type = type,
    title = title,
    lastMessageAt = lastMessageAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    type = type,
    title = title,
    lastMessageAt = lastMessageAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)