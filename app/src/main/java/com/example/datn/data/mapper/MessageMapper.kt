package com.example.datn.data.mapper

import com.example.datn.data.local.entities.MessageEntity
import com.example.datn.domain.models.Message

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    senderId = senderId,
    recipientId = recipientId,
    content = content,
    sentAt = sentAt,
    isRead = isRead,
    conversationId = conversationId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    senderId = senderId,
    recipientId = recipientId,
    content = content,
    sentAt = sentAt,
    isRead = isRead,
    conversationId = conversationId,
    createdAt = createdAt,
    updatedAt = updatedAt
)