package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@Entity(tableName = "minigame_option")
data class MiniGameOptionEntity(
    @PrimaryKey
    val id: String,
    val miniGameQuestionId: String,
    val content: String,
    @JsonProperty("correct")
    var isCorrect: Boolean = false,
    val order: Int,
    val mediaUrl: String? = null,
    // For PUZZLE: hint characters to show (e.g., "a__le" for "apple")
    val hint: String? = null,
    // For MATCHING: the ID of the paired option
    val pairId: String? = null,
    // For MATCHING: the content of the pair (for display purposes)
    val pairContent: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)