package com.example.datn.domain.models

import java.time.Instant

data class MiniGame(
    val id: String,
    val teacherId: String,
    val lessonId: String, 
    val title: String,
    val description: String,
    val level: Level,
    val createdAt: Instant,
    val updatedAt: Instant
)