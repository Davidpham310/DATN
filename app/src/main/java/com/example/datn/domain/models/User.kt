package com.example.datn.domain.models

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val phone: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)
enum class UserRole(val displayName: String) {
    TEACHER("Giáo viên"),
    PARENT("Phụ huynh"),
    STUDENT("Học sinh");

    companion object {
        fun fromDisplayName(displayName: String): UserRole? {
            return values().find { it.displayName == displayName }
        }
        fun fromString(role: String): UserRole? {
            return values().find { it.name.equals(role, ignoreCase = true) }
        }
    }
}