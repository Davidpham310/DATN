package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey val id: String,
    val assignmentId: String,
    val studentId: String,
    val fileUrl: String? = null,
    val submittedAt: Long? = null,
    val grade: Float? = null,
    val comment: String? = null
)
