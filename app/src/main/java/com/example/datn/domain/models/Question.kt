package com.example.datn.domain.models

data class Question(
    val id: String,
    val testId: String,
    val questionText: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String?,
    val type: String
)
