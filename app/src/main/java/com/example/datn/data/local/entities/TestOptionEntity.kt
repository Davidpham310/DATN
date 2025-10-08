package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "test_option")
data class TestOptionEntity(
    @PrimaryKey
    val id: String,
    val testQuestionId: String,
    val content: String,
    val isCorrect: Boolean,
    val order: Int,
    val mediaUrl: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)