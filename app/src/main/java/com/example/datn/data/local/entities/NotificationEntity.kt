package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.NotificationType
import java.time.Instant

@Entity(tableName = "notification")
data class NotificationEntity(
    @PrimaryKey
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