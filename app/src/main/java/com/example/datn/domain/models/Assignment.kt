package com.example.datn.domain.models

data class Assignment(
    val id: String,
    val classId: String,
    val title: String,
    val description: String?,
    val deadline: Long?,
    val allowLate: Boolean = false,
    val createdAt: Long? = null
)