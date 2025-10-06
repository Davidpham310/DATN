package com.example.datn.data.mapper

import com.example.datn.data.local.entities.UserEntity

import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole


fun UserEntity.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        role = UserRole.fromString(role) ?: UserRole.STUDENT,
        avatarUrl = avatarUrl,
        phone = phone,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        role = role.name,
        avatarUrl = avatarUrl,
        phone = phone,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}