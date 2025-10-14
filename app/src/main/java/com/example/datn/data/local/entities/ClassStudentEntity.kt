package com.example.datn.data.local.entities

import androidx.room.Entity
import java.time.Instant

@Entity(
    tableName = "class_student",
    primaryKeys = ["classId", "studentId"]
)
data class ClassStudentEntity(
    val classId: String,
    val studentId: String,
    val joinedAt: Instant = Instant.now(),
    val isLocked: Boolean = false
)