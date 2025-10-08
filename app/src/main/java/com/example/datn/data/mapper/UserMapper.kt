package com.example.datn.data.mapper

import com.example.datn.data.local.entities.UserEntity

import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole


fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    role = role,
    avatarUrl = avatarUrl,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = email,
    role = role,
    avatarUrl = avatarUrl,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)