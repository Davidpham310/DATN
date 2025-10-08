package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "student")
data class StudentEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val dateOfBirth: LocalDate,
    val gradeLevel: String,
    val createdAt: Instant,
    val updatedAt: Instant
)