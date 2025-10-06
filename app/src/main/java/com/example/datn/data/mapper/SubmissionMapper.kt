package com.example.datn.data.mapper

import com.example.datn.data.local.entities.SubmissionEntity
import com.example.datn.domain.models.Submission

fun SubmissionEntity.toDomain(): Submission {
    return Submission(
        id = id,
        assignmentId = assignmentId,
        studentId = studentId,
        fileUrl = fileUrl,
        submittedAt = submittedAt,
        grade = grade,
        comment = comment
    )
}

fun Submission.toEntity(): SubmissionEntity {
    return SubmissionEntity(
        id = id,
        assignmentId = assignmentId,
        studentId = studentId,
        fileUrl = fileUrl,
        submittedAt = submittedAt,
        grade = grade,
        comment = comment
    )
}
