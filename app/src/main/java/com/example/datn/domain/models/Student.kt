package com.example.datn.domain.models

import java.time.Instant
import java.time.LocalDate

data class Student(
    val id: String,
    val userId: String,
    val dateOfBirth: LocalDate,
    val gradeLevel: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
