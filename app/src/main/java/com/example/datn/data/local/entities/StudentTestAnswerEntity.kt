package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.StudentTestAnswer
import java.time.Instant

@Entity(tableName = "student_test_answers")
data class StudentTestAnswerEntity(
    @PrimaryKey val id: String,
    val resultId: String,
    val questionId: String,
    val answer: String,
    val isCorrect: Boolean,
    val earnedScore: Double,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomainModel(): StudentTestAnswer {
        return StudentTestAnswer(
            id = id,
            resultId = resultId,
            questionId = questionId,
            answer = answer,
            isCorrect = isCorrect,
            earnedScore = earnedScore,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt)
        )
    }
}

fun StudentTestAnswer.toEntity(): StudentTestAnswerEntity {
    return StudentTestAnswerEntity(
        id = id,
        resultId = resultId,
        questionId = questionId,
        answer = answer,
        isCorrect = isCorrect,
        earnedScore = earnedScore,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli()
    )
}
