package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "lesson")
data class LessonEntity(
    @PrimaryKey
    val id: String,
    val teacherId: String,
    val classId: String,
    val title: String,
    val description: String? = null,
    val contentLink: String? = null,
    val order: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)