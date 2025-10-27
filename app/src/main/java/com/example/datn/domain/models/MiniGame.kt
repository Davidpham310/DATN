package com.example.datn.domain.models

import java.time.Instant

data class MiniGame(
    val id: String,
    val teacherId: String,
    val lessonId: String, // Liên kết với bài học
    val title: String,
    val description: String,
    val gameType: GameType,
    val contentUrl: String? = null,
    val level: Level,
    val createdAt: Instant,
    val updatedAt: Instant
)