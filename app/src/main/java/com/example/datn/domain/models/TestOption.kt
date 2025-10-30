package com.example.datn.domain.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class TestOption(
    val id: String,
    val testQuestionId: String,
    val content: String,
    @JsonProperty("correct")
    val isCorrect: Boolean,
    val order: Int,
    val mediaUrl: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
