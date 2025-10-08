package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.QuestionType
import java.time.Instant

@Entity(tableName = "minigame_question")
data class MiniGameQuestionEntity(
    @PrimaryKey
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
