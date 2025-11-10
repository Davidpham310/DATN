package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.StudentMiniGameAnswer
import java.time.Instant

@Entity(tableName = "student_minigame_answer")
data class StudentMiniGameAnswerEntity(
    @PrimaryKey
    val id: String,
    val resultId: String,  // Foreign key to StudentMiniGameResultEntity
    val questionId: String,
    val answer: String,  // JSON string of answer
    val isCorrect: Boolean,
    val earnedScore: Double,
    val createdAt: Instant,
    val updatedAt: Instant
)

// Mapper: Entity -> Domain
fun StudentMiniGameAnswerEntity.toDomain(): StudentMiniGameAnswer {
    return StudentMiniGameAnswer(
        id = id,
        resultId = resultId,
        questionId = questionId,
        answer = answer,
        isCorrect = isCorrect,
        earnedScore = earnedScore,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Mapper: Domain -> Entity
fun StudentMiniGameAnswer.toEntity(): StudentMiniGameAnswerEntity {
    return StudentMiniGameAnswerEntity(
        id = id,
        resultId = resultId,
        questionId = questionId,
        answer = answer,
        isCorrect = isCorrect,
        earnedScore = earnedScore,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
