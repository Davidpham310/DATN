package com.example.datn.domain.models

data class NotificationItem(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val type: String,
    val createdAt: Long?,
    val isRead: Boolean = false
)
