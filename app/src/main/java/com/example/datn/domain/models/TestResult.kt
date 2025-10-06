package com.example.datn.domain.models

data class TestResult(
    val id: String,
    val testId: String,
    val studentId: String,
    val score: Float?,
    val submittedAt: Long?,
    val feedback: String?
)
