package com.example.datn.domain.models

data class Lesson(
    val id: String,
    val classId: String,
    val title: String,
    val description: String?,
    val videoUrl: String?,
    val documentUrl: String?,
    val isPublished: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long? = null
)