package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.UserRole
import java.time.Instant

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)