package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profiles")
data class StudentProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val parentId: String?,
    val birthDate: String?,
    val grade: String?,
    val school: String?,
    val createdAt: Long? = null
)
