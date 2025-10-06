package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val teacherId: String,
    val subject: String? = null,
    val inviteCode: String? = null,
    val status: String = "OPEN",
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)
