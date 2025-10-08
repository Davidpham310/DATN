package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "class")
data class ClassEntity(
    @PrimaryKey
    val id: String,
    val teacherId: String,
    val name: String,
    val classCode: String,
    val gradeLevel: String? = null,
    val subject: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)