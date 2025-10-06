package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val type: String,
    val createdAt: Long? = null,
    val isRead: Boolean = false
)
