package com.example.datn.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "class_members",
    primaryKeys = ["id"]
)
data class ClassMemberEntity(
    val id: String,
    val classId: String,
    val studentId: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val joinedAt: Long? = null
)
