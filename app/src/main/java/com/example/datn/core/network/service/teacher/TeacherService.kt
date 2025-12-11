package com.example.datn.core.network.service.teacher

import com.example.datn.core.network.service.firestore.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Teacher
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TeacherService @Inject constructor() :
    BaseFirestoreService<Teacher>(collectionName = "teachers", clazz = Teacher::class.java) {

    // Override getById để tìm theo field 'id' thay vì Firestore document ID,
    // giúp tương thích với dữ liệu cũ có documentId khác với trường 'id'.
    override suspend fun getById(id: String): Teacher? {
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

    // Lấy giáo viên theo user ID
    suspend fun getTeacherByUserId(userId: String): Teacher? {
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
