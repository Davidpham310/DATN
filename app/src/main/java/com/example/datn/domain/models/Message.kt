package com.example.datn.domain.models

data class Message(
    val id: String,
    val senderId: String,
    val receiverId: String?,
    val classId: String?,
    val content: String,
    val sentAt: Long?,
    val isRead: Boolean = false
)
