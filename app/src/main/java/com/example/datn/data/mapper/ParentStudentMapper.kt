package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ParentStudentEntity
import com.example.datn.domain.models.ParentStudent

fun ParentStudentEntity.toDomain(): ParentStudent = ParentStudent(
    parentId = parentId,
    studentId = studentId,
    relationship = relationship,
    linkedAt = linkedAt,
    isPrimaryGuardian = isPrimaryGuardian
)

fun ParentStudent.toEntity(): ParentStudentEntity = ParentStudentEntity(
    parentId = parentId,
    studentId = studentId,
    relationship = relationship,
    linkedAt = linkedAt,
    isPrimaryGuardian = isPrimaryGuardian
)