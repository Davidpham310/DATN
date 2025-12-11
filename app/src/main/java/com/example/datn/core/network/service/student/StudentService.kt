package com.example.datn.core.network.service.student

import com.example.datn.core.network.service.firestore.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Student
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StudentService @Inject constructor() :
    BaseFirestoreService<Student>(collectionName = "students", clazz = Student::class.java) {

    // Override getById để tìm theo field 'id' thay vì Firestore document ID,
    // giúp tương thích với dữ liệu cũ có documentId khác với trường 'id'.
    override suspend fun getById(id: String): Student? {
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

    // Lấy danh sách học sinh theo lớp
    suspend fun getStudentsByClassId(classId: String): List<Student> {
        val snapshot = collectionRef
            .whereEqualTo("classId", classId)
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

    // Lấy danh sách học sinh theo user ID
    suspend fun getStudentByUserId(userId: String): Student? {
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

    // Lấy danh sách học sinh theo phụ huynh
    suspend fun getStudentsByParentId(parentId: String): List<Student> {
        val snapshot = collectionRef
            .whereEqualTo("parentId", parentId)
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
