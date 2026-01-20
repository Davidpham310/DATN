    package com.example.datn.domain.models

    import java.time.Instant

    data class TestQuestion(
        val id: String,
        val testId: String,
        val content: String,
        val score: Double,
        val questionType: QuestionType,
        val mediaUrl: String? = null,
        val timeLimit: Int = 0,
        val order: Int,
        val createdAt: Instant,
        val updatedAt: Instant
    )
