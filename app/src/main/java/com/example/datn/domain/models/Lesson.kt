package com.example.datn.domain.models

import java.time.Instant

data class Lesson(
    val id: String,
    val teacherId: String,
    val classId: String,
    val title: String,
    val description: String? = null,
    val contentLink: String? = null,
    val order: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)