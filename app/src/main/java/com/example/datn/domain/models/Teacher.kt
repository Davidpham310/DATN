package com.example.datn.domain.models

import java.time.Instant

data class Teacher(
    val id: String,
    val userId: String,
    val specialization: String,
    val level: String,
    val experienceYears: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
