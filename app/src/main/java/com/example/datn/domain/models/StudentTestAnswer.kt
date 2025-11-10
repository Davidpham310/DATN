package com.example.datn.domain.models

import com.google.firebase.firestore.PropertyName
import java.time.Instant

/**
 * Represents a student's answer to a specific test question
 */
data class StudentTestAnswer(
    val id: String,
    val resultId: String,  // Links to StudentTestResult
    val questionId: String,
    val answer: String,     // Serialized answer (optionId, optionIds, or text)
    @field:PropertyName("correct")
    val isCorrect: Boolean,
    val earnedScore: Double,
    val createdAt: Instant,
    val updatedAt: Instant
)
