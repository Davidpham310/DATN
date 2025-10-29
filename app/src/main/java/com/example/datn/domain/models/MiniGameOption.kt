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
    val createdAt: Instant,
    val updatedAt: Instant
)