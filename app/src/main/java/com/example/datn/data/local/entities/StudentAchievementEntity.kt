package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_achievements")
data class StudentAchievementEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val achievementId: String,
    val earnedAt: Long? = null
)
