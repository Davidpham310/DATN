package com.example.datn.domain.models

import java.time.Instant

data class Test(
    val id: String,
    val classId: String,
    val lessonId: String,
    val title: String,
    val description: String? = null,
    val totalScore: Double,
    val startTime: Instant,
    val endTime: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
