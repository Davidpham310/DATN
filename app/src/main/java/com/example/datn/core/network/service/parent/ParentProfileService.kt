package com.example.datn.core.network.service.parent

import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Parent
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ParentProfileService @Inject constructor() :
    BaseFirestoreService<Parent>(collectionName = "parents", clazz = Parent::class.java) {

    // Override getById để tìm theo field 'id' thay vì Firestore document ID,
    // giúp tương thích với dữ liệu cũ có documentId khác với trường 'id'.
    override suspend fun getById(id: String): Parent? {
        val snapshot = collectionRef
            .whereEqualTo("id", id)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let {
            try {
                it.internalToDomain(clazz)
            } catch (_: Exception) {
                null
            }
        }
    }

    // Lấy phụ huynh theo user ID
    suspend fun getParentByUserId(userId: String): Parent? {
        val snapshot = collectionRef
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let {
            try {
                it.internalToDomain(clazz)
            } catch (_: Exception) {
                null
            }
        }
    }
}
