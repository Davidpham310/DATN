package com.example.datn.domain.models

import java.time.Instant

data class LessonContent(
    val id: String,
    val lessonId: String,
    val title: String,
    val contentType: ContentType,
    val content: String,
    val order: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
