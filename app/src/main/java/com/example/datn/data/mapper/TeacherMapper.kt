package com.example.datn.data.mapper

import com.example.datn.data.local.entities.TeacherEntity
import com.example.datn.domain.models.Teacher

fun TeacherEntity.toDomain(): Teacher = Teacher(
    id = id,
    userId = userId,
    specialization = specialization,
    level = level,
    experienceYears = experienceYears,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Teacher.toEntity(): TeacherEntity = TeacherEntity(
    id = id,
    userId = userId,
    specialization = specialization,
    level = level,
    experienceYears = experienceYears,
    createdAt = createdAt,
    updatedAt = updatedAt
)