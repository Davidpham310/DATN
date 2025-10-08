package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "student_lesson_progress")
data class StudentLessonProgressEntity(
    @PrimaryKey
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