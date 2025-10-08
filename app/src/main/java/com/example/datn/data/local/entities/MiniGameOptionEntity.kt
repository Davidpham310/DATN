package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "minigame_option")
data class MiniGameOptionEntity(
    @PrimaryKey
    val id: String,
    val miniGameQuestionId: String,
    val content: String,
    val isCorrect: Boolean,
    val order: Int,
    val mediaUrl: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)