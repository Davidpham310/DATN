package com.example.datn.core.network.service.user

import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserService @Inject constructor() :
    BaseFirestoreService<User>(
        collectionName = "users",
        clazz = User::class.java
    ) {
    // Lấy user theo id
    suspend fun getUserById(userId: String): User? {
        val snapshot = collectionRef
            .whereEqualTo("id", userId)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull()
        return doc?.let { it.internalToDomain(clazz) }
    }

    // Lấy user theo email
    suspend fun getUserByEmail(email: String): User? {
        val snapshot = collectionRef
            .whereEqualTo("email", email)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull()
        return doc?.let { it.internalToDomain(clazz) }
    }

    // Cập nhật avatar
    suspend fun updateAvatar(userId: String, avatarUrl: String) {
        collectionRef.document(userId)
            .update("avatarUrl", avatarUrl)
            .await()
    }

    // Lấy tất cả user theo role
    suspend fun getUsersByRole(role: String): List<User> {
        val snapshot = collectionRef
            .whereEqualTo("role", role)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(clazz)
            } catch (_: Exception) {
                null
            }
        }
    }
}