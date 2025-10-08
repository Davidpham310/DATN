package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "teacher")
data class TeacherEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val specialization: String,
    val level: String,
    val experienceYears: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)