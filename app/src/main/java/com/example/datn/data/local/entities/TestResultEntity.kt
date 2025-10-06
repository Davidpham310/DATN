package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val studentId: String,
    val score: Float? = null,
    val submittedAt: Long? = null,
    val feedback: String? = null
)
