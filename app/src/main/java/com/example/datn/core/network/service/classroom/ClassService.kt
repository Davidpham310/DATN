package com.example.datn.core.network.service.classroom

import android.util.Log
import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Class
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ClassService @Inject constructor() :
    BaseFirestoreService<Class>(
        collectionName = "classes",
        clazz = Class::class.java
    ) {


    // Lấy lớp theo ID
    suspend fun getClassById(classId: String): Class? {
        val snapshot = collectionRef
            .whereEqualTo("id", classId)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull()
        return doc?.internalToDomain(clazz)
    }

    // Lấy tất cả lớp của giáo viên
    suspend fun getClassesByTeacher(teacherId: String): List<Class> {
        val snapshot = collectionRef
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        snapshot.documents.forEach { doc ->
            Log.d("ClassService", "Raw Firestore data: ${doc.data}")
        }
        // Map sang Class
        val classes = snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(clazz)
            } catch (e: Exception) {
                Log.e("ClassService", "Failed to parse doc ${it.id}", e)
                null
            }
        }

        Log.d("ClassService", "Mapped classes: $classes")
        return snapshot.documents.mapNotNull { it.internalToDomain(clazz) }
    }

    // Lấy tất cả lớp học sinh tham gia
    suspend fun getClassesByStudent(studentId: String): List<Class> {
        val snapshot = collectionRef
            .whereArrayContains("studentIds", studentId) // Nếu lưu array studentIds trong class
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.internalToDomain(clazz) }
    }

    // Thêm học sinh vào lớp
    suspend fun addStudentToClass(classId: String, studentId: String) {
        val studentRef = collectionRef.document(classId)
        studentRef.update("studentIds", com.google.firebase.firestore.FieldValue.arrayUnion(studentId)).await()
    }

    // Xóa học sinh khỏi lớp
    suspend fun removeStudentFromClass(classId: String, studentId: String) {
        val studentRef = collectionRef.document(classId)
        studentRef.update("studentIds", com.google.firebase.firestore.FieldValue.arrayRemove(studentId)).await()
    }

    // Thêm lớp mới
    suspend fun addClass(classObj: Class): Class {
        val docRef = if (classObj.id.isNotEmpty()) {
            collectionRef.document(classObj.id)
        } else {
            collectionRef.document() // Firestore tự tạo id
        }
        val classWithId = classObj.copy(id = docRef.id)
        docRef.set(classWithId).await()
        return classWithId
    }

    // Cập nhật lớp
    suspend fun updateClass(classId: String, classObj: Class) {
        collectionRef.document(classId).set(classObj).await()
    }

    // Xóa lớp
    suspend fun deleteClass(classId: String) {
        collectionRef.document(classId).delete().await()
    }
}
