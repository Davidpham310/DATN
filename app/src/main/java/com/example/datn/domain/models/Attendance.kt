package com.example.datn.domain.models

data class Attendance(
    val id: String,
    val classId: String,
    val studentId: String,
    val date: String,
    val status: String,
    val note: String?
)
