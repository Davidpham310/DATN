package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ClassEntity
import com.example.datn.domain.models.Class

fun ClassEntity.toDomain(): Class = Class(
    id = id,
    teacherId = teacherId,
    name = name,
    classCode = classCode,
    gradeLevel = gradeLevel,
    subject = subject,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Class.toEntity(): ClassEntity = ClassEntity(
    id = id,
    teacherId = teacherId,
    name = name,
    classCode = classCode,
    gradeLevel = gradeLevel,
    subject = subject,
    createdAt = createdAt,
    updatedAt = updatedAt
)