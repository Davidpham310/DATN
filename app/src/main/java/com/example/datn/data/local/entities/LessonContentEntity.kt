package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.ContentType
import java.time.Instant

@Entity(tableName = "lesson_content")
data class LessonContentEntity(
    @PrimaryKey
    val id: String,
    val lessonId: String,
    val title: String,
    val contentType: ContentType,
    val content: String,
    val order: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)