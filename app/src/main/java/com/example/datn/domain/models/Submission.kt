package com.example.datn.domain.models

data class Submission(
    val id: String,
    val assignmentId: String,
    val studentId: String,
    val fileUrl: String?,
    val submittedAt: Long?,
    val grade: Float?,
    val comment: String?
)