package com.example.datn.domain.models

import java.time.Instant

data class StudentTestResult(
    val id: String,
    val studentId: String,
    val testId: String,
    val score: Double,
    val completionStatus: TestStatus,
    val submissionTime: Instant,
    val durationSeconds: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)