package com.example.datn.domain.models

data class StudentProfile(
    val id: String,
    val userId: String,
    val parentId: String?,
    val birthDate: String?,
    val grade: String?,
    val school: String?,
    val createdAt: Long? = null
)