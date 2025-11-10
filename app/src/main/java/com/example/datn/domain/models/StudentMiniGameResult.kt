package com.example.datn.domain.models

import java.time.Instant

/**
 * Represents a student's result for a mini game attempt
 * Unlike Test, students can play mini games multiple times
 */
data class StudentMiniGameResult(
    val id: String,
    val studentId: String,
    val miniGameId: String,
    val score: Double,
    val maxScore: Double,
    val completionStatus: CompletionStatus,
    val submissionTime: Instant,
    val durationSeconds: Long,
    val attemptNumber: Int = 1,  // Track attempt number for replay
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class CompletionStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED
}
