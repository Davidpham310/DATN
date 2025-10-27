package com.example.datn.domain.models

import java.time.Instant

data class StudentGameResult(
    val id: String,
    val studentId: String,
    val miniGameId: String,
    val score: Double,
    val maxScore: Double, // Tổng điểm tối đa
    val correctAnswers: Int,
    val totalQuestions: Int,
    val submissionTime: Instant,
    val durationSeconds: Long, // Thời gian làm bài
    val attempts: Int, // Số lần thử
    val answers: Map<String, List<String>>, // questionId -> List of optionIds
    val createdAt: Instant,
    val updatedAt: Instant
)