package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "daily_study_time")
data class DailyStudyTimeEntity(
    @PrimaryKey
    val id: String,
    val studentId: String,
    val date: LocalDate,
    val durationSeconds: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)