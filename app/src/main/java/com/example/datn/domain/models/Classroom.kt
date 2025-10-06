package com.example.datn.domain.models

data class Classroom(
    val id: String,
    val name: String,
    val description: String?,
    val teacherId: String,
    val subject: String?,
    val inviteCode: String?,
    val status: String?,
    val createdAt: Long?,
    val updatedAt: Long?
)