package com.example.datn.data.mapper

import com.example.datn.data.local.entities.ClassStudentEntity
import com.example.datn.domain.models.ClassStudent

/**
 * Mapper giữa ClassStudentEntity và ClassStudent domain model
 */

fun ClassStudentEntity.toDomain(): ClassStudent {
    return ClassStudent(
        classId = this.classId,
        studentId = this.studentId,
        enrolledDate = this.enrolledDate,
        enrollmentStatus = this.enrollmentStatus,
        approvedBy = this.approvedBy,
        rejectionReason = this.rejectionReason
    )
}

fun ClassStudent.toEntity(): ClassStudentEntity {
    return ClassStudentEntity(
        classId = this.classId,
        studentId = this.studentId,
        enrollmentStatus = this.enrollmentStatus,
        enrolledDate = this.enrolledDate,
        approvedBy = this.approvedBy,
        rejectionReason = this.rejectionReason,
        isLocked = false // Default value
    )
}

fun List<ClassStudentEntity>.toDomainList(): List<ClassStudent> {
    return this.map { it.toDomain() }
}

fun List<ClassStudent>.toEntityList(): List<ClassStudentEntity> {
    return this.map { it.toEntity() }
}
