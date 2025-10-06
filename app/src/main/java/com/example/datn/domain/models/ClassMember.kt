package com.example.datn.domain.models

data class ClassMember(
    val id: String,
    val classId: String,
    val studentId: String,
    val status: String,
    val joinedAt: Long?
)
