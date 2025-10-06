package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val title: String,
    val description: String? = null,
    val deadline: Long? = null,
    val allowLate: Boolean = false,
    val createdAt: Long? = null
)
