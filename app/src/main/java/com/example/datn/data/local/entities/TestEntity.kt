package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "test")
data class TestEntity(
    @PrimaryKey
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
