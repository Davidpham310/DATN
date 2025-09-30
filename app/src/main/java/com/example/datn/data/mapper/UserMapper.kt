package com.example.datn.data.mapper

import com.example.datn.data.local.entities.UserEntity

import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole


fun UserEntity.toDomain(): User {
    return User(
        id = this.id,
        name = this.name,
        role = UserRole.valueOf(this.role),
        email = this.email
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = this.id,
        name = this.name,
        role = this.role.name,
        email = this.email
    )
}