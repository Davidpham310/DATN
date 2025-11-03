package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ParentEntity
import com.example.datn.domain.models.Parent

fun ParentEntity.toDomain(): Parent = Parent(
    id = id,
    userId = userId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Parent.toEntity(): ParentEntity = ParentEntity(
    id = id,
    userId = userId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

