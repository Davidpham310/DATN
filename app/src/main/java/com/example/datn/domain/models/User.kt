package com.example.datn.domain.models

import java.time.Instant
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
