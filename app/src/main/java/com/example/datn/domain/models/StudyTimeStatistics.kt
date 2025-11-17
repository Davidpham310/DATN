package com.example.datn.domain.models

import java.time.LocalDate

data class StudyTimeStatistics(
    val studentId: String,
    val todaySeconds: Long,
    val weekSeconds: Long,
    val monthSeconds: Long,
    val totalSeconds: Long,
    val dailyRecords: List<DailyStudyTime>
)
