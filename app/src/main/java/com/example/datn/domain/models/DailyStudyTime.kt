package com.example.datn.domain.models

import java.time.Instant
import java.time.LocalDate

data class DailyStudyTime(
    val id: String,
    val studentId: String,
    val date: LocalDate,
    val durationSeconds: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)