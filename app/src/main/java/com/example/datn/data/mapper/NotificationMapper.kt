package com.example.datn.data.mapper

import com.example.datn.data.local.entities.NotificationEntity
import com.example.datn.domain.models.Notification

fun NotificationEntity.toDomain(): Notification = Notification(
    id = id,
    userId = userId,
    senderId = senderId,
    type = type,
    title = title,
    content = content,
    referenceObjectId = referenceObjectId,
    referenceObjectType = referenceObjectType,
    isRead = isRead,
    createdAt = createdAt
)

fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    userId = userId,
    senderId = senderId,
    type = type,
    title = title,
    content = content,
    referenceObjectId = referenceObjectId,
    referenceObjectType = referenceObjectType,
    isRead = isRead,
    createdAt = createdAt
)