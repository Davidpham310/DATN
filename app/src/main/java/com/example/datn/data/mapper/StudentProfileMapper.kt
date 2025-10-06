package com.example.datn.data.mapper

import com.example.datn.data.local.entities.StudentProfileEntity
import com.example.datn.domain.models.StudentProfile

fun StudentProfileEntity.toDomain(): StudentProfile {
    return StudentProfile(
        id = id,
        userId = userId,
        parentId = parentId,
        birthDate = birthDate,
        grade = grade,
        school = school,
        createdAt = createdAt
    )
}

fun StudentProfile.toEntity(): StudentProfileEntity {
    return StudentProfileEntity(
        id = id,
        userId = userId,
        parentId = parentId,
        birthDate = birthDate,
        grade = grade,
        school = school,
        createdAt = createdAt
    )
}
