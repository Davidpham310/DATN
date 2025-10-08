package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.datn.domain.models.TestStatus
import java.time.Instant

@Entity(tableName = "student_test_result")
data class StudentTestResultEntity(
    @PrimaryKey
    val id: String,
    val studentId: String,
    val testId: String,
    val score: Double,
    val completionStatus: TestStatus,
    val submissionTime: Instant,
    val durationSeconds: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)