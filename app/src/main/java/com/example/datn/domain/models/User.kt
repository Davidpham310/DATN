package com.example.datn.domain.models

data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole
)
enum class UserRole(val displayName: String) {
    TEACHER("Giáo viên"),
    PARENT("Phụ huynh"),
    STUDENT("Học sinh");

    companion object {
        fun fromDisplayName(displayName: String): UserRole? {
            return values().find { it.displayName == displayName }
        }
    }
}