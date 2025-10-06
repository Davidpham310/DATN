package com.example.datn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tests")
data class TestEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val title: String,
    val description: String? = null,
    val duration: Int? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val allowRetry: Boolean = false,
    val allowViewAnswer: Boolean = false
)
