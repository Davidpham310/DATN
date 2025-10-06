package com.example.datn.data.mapper

import com.example.datn.data.local.entities.MessageEntity
import com.example.datn.domain.models.Message

fun MessageEntity.toDomain(): Message {
    return Message(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        classId = classId,
        content = content,
        sentAt = sentAt,
        isRead = isRead
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        classId = classId,
        content = content,
        sentAt = sentAt,
        isRead = isRead
    )
}
