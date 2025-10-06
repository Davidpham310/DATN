package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ClassEntity
import com.example.datn.domain.models.Classroom

fun ClassEntity.toDomain() = Classroom(
    id = id,
    name = name,
    description = description,
    teacherId = teacherId,
    subject = subject,
    inviteCode = inviteCode,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Classroom.toEntity() = ClassEntity(
    id = id,
    name = name,
    description = description,
    teacherId = teacherId,
    subject = subject,
    inviteCode = inviteCode,
    status = status ?: "OPEN",
    createdAt = createdAt,
    updatedAt = updatedAt
)
