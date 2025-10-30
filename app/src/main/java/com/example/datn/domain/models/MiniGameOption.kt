package com.example.datn.domain.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class MiniGameOption(
    val id: String,
    val miniGameQuestionId: String,
    val content: String,
    val mediaUrl : String? = null,
    @JsonProperty("correct")
    val isCorrect: Boolean,
    val order: Int,
    // For PUZZLE: hint characters to show (e.g., "a__le" for "apple")
    val hint: String? = null,
    // For MATCHING: the ID of the paired option
    val pairId: String? = null,
    // For MATCHING: the content of the pair (for display purposes)
    val pairContent: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)