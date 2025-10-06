package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)