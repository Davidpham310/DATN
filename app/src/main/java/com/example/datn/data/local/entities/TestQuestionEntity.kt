package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.QuestionType
import java.time.Instant

@Entity(tableName = "test_question")
data class TestQuestionEntity(
    @PrimaryKey
    val id: String,
    val testId: String,
    val content: String,
    val score: Double,
    val questionType: QuestionType,
    val mediaUrl: String? = null,
    val timeLimit: Int,
    val order: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)