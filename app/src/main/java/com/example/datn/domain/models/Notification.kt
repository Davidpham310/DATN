package com.example.datn.domain.models

import java.time.Instant

data class Notification(
    val id: String,
    val userId: String,
    val senderId: String?,
    val type: NotificationType,
    val title: String,
    val content: String,
    val referenceObjectId: String?,
    val referenceObjectType: String?,
    val isRead: Boolean,
    val createdAt: Instant
)
