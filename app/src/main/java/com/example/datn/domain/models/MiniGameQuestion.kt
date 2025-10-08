package com.example.datn.domain.models

import java.time.Instant

data class MiniGameQuestion(
    val id: String,
    val miniGameId: String,
    val content: String,
    val questionType: QuestionType,
    val score: Double,
    val timeLimit: Long,
    val order: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)