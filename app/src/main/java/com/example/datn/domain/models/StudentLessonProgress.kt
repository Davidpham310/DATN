package com.example.datn.domain.models

import java.time.Instant

data class StudentLessonProgress(
    val id: String,
    val studentId: String,
    val lessonId: String,
    val progressPercentage: Int,
    val lastAccessedContentId: String?,
    val lastAccessedAt: Instant,
    val isCompleted: Boolean,
    val timeSpentSeconds: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)