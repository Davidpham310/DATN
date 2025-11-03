package com.example.datn.core.network.service.student

import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Student
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StudentService @Inject constructor() :
    BaseFirestoreService<Student>(
        collectionName = "students",
        clazz = Student::class.java
    ) {
    
    // Expose generateDocumentId for creating new student IDs
    fun generateStudentId(): String = generateDocumentId()
    
    // Lấy thông tin học sinh theo userId
    suspend fun getStudentByUserId(userId: String): Student? {
        val snapshot = collectionRef
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .await()
        
        return snapshot.documents.firstOrNull()?.let {
            it.internalToDomain(clazz)
        }
    }
    
    // Lấy thông tin học sinh theo ID
    suspend fun getStudentById(studentId: String): Student? {
        return getById(studentId)
    }
    
    // Cập nhật thông tin học sinh
    suspend fun updateStudent(studentId: String, student: Student): Boolean {
        return try {
            update(studentId, student)
            true
        } catch (e: Exception) {
            false
        }
    }
}

