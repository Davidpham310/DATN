package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String?,
    val classId: String?,
    val content: String,
    val sentAt: Long? = null,
    val isRead: Boolean = false
)
