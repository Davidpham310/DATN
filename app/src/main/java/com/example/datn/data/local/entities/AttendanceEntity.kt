package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendances")
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val studentId: String,
    val date: String,
    val status: String,
    val note: String? = null
)
