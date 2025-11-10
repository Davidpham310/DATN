package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.CompletionStatus
import com.example.datn.domain.models.StudentMiniGameResult
import java.time.Instant

@Entity(tableName = "student_minigame_result")
data class StudentMiniGameResultEntity(
    @PrimaryKey
    val id: String,
    val studentId: String,
    val miniGameId: String,
    val score: Double,
    val maxScore: Double,
    val completionStatus: CompletionStatus,
    val submissionTime: Instant,
    val durationSeconds: Long,
    val attemptNumber: Int = 1,  // For unlimited replay
    val createdAt: Instant,
    val updatedAt: Instant
)

// Mapper: Entity -> Domain
fun StudentMiniGameResultEntity.toDomain(): StudentMiniGameResult {
    return StudentMiniGameResult(
        id = id,
        studentId = studentId,
        miniGameId = miniGameId,
        score = score,
        maxScore = maxScore,
        completionStatus = completionStatus,
        submissionTime = submissionTime,
        durationSeconds = durationSeconds,
        attemptNumber = attemptNumber,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Mapper: Domain -> Entity
fun StudentMiniGameResult.toEntity(): StudentMiniGameResultEntity {
    return StudentMiniGameResultEntity(
        id = id,
        studentId = studentId,
        miniGameId = miniGameId,
        score = score,
        maxScore = maxScore,
        completionStatus = completionStatus,
        submissionTime = submissionTime,
        durationSeconds = durationSeconds,
        attemptNumber = attemptNumber,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
