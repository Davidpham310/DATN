package com.example.datn.data.mapper

import com.example.datn.data.local.entities.StudentEntity
import com.example.datn.domain.models.Student

fun StudentEntity.toDomain(): Student = Student(
    id = id,
    userId = userId,
    dateOfBirth = dateOfBirth,
    gradeLevel = gradeLevel,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Student.toEntity(): StudentEntity = StudentEntity(
    id = id,
    userId = userId,
    dateOfBirth = dateOfBirth,
    gradeLevel = gradeLevel,
    createdAt = createdAt,
    updatedAt = updatedAt
)