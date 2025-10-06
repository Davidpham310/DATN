package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val title: String,
    val description: String? = null,
    val videoUrl: String? = null,
    val documentUrl: String? = null,
    val isPublished: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long? = null
)
