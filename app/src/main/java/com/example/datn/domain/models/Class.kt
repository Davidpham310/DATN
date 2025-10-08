package com.example.datn.domain.models

import java.time.Instant

data class Class(
    val id: String,
    val teacherId: String,
    val name: String,
    val classCode: String,
    val gradeLevel: String?= null,
    val subject : String?= null,
    val createdAt: Instant,
    val updatedAt: Instant
)