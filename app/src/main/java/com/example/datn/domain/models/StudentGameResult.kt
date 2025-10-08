package com.example.datn.domain.models

import java.time.Instant

data class StudentGameResult(
    val id: String,
    val studentId: String,
    val miniGameId: String,
    val score: Double,
    val submissionTime: Instant,
    val durationSeconds: Long, // Thời gian học sinh đã dành để chơi game (tính bằng giây)
    val attempts: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)