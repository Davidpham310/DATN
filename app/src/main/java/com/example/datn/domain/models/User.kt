package com.example.datn.domain.models

data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole
)
enum class UserRole {
    STUDENT, PARENT, TEACHER
}