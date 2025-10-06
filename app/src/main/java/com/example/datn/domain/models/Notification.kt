package com.example.datn.domain.models

data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val type: String,
    val createdAt: Long? = null,
    val isRead: Boolean = false
)
