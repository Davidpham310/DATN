package com.example.datn.data.mapper

import com.example.datn.data.local.entities.NotificationEntity
import com.example.datn.domain.models.Notification

fun NotificationEntity.toDomain(): Notification {
    return Notification(
        id = id,
        userId = userId,
        title = title,
        content = content,
        type = type,
        createdAt = createdAt,
        isRead = isRead
    )
}

fun Notification.toEntity(): NotificationEntity {
    return NotificationEntity(
        id = id,
        userId = userId,
        title = title,
        content = content,
        type = type,
        createdAt = createdAt,
        isRead = isRead
    )
}
