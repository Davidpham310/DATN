package com.example.datn.data.mapper

import com.example.datn.data.local.entities.AssignmentEntity
import com.example.datn.domain.models.Assignment

fun AssignmentEntity.toDomain(): Assignment {
    return Assignment(
        id = id,
        classId = classId,
        title = title,
        description = description,
        deadline = deadline,
        allowLate = allowLate,
        createdAt = createdAt
    )
}

fun Assignment.toEntity(): AssignmentEntity {
    return AssignmentEntity(
        id = id,
        classId = classId,
        title = title,
        description = description,
        deadline = deadline,
        allowLate = allowLate,
        createdAt = createdAt
    )
}
