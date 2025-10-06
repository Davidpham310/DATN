package com.example.datn.domain.models

data class StudentAchievement(
    val id: String,
    val studentId: String,
    val achievementId: String,
    val earnedAt: Long?
)
