package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val classId: String?,
    val title: String,
    val startTime: Long?,
    val endTime: Long?,
    val type: String?,
    val createdAt: Long? = null
)
