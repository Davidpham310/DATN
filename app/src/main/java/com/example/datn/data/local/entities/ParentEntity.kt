package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "parent")
data class ParentEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val createdAt: Instant,
    val updatedAt: Instant
)