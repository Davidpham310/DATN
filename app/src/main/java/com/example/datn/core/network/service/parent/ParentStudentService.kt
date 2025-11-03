package com.example.datn.core.network.service.parent

import com.example.datn.domain.models.ParentStudent
import com.example.datn.domain.models.RelationshipType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class ParentStudentService @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val parentStudentRef = firestore.collection("parent_student")
    
    // Tạo liên kết parent-student
    suspend fun createParentStudentLink(
        parentId: String,
        studentId: String,
        relationship: RelationshipType,
        isPrimaryGuardian: Boolean
    ): Boolean {
        return try {
            val parentStudent = hashMapOf(
                "parentId" to parentId,
                "studentId" to studentId,
                "relationship" to relationship.name,
                "linkedAt" to System.currentTimeMillis(),
                "isPrimaryGuardian" to isPrimaryGuardian
            )
            val docId = "${parentId}_${studentId}"
            parentStudentRef.document(docId).set(parentStudent).await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Lấy danh sách parent-student links của một parent
    suspend fun getParentStudentLinks(parentId: String): List<ParentStudent> {
        val snapshot = parentStudentRef
            .whereEqualTo("parentId", parentId)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                ParentStudent(
                    parentId = data["parentId"] as? String ?: "",
                    studentId = data["studentId"] as? String ?: "",
                    relationship = RelationshipType.valueOf(
                        (data["relationship"] as? String) ?: "GUARDIAN"
                    ),
                    linkedAt = Instant.ofEpochMilli(
                        (data["linkedAt"] as? Long) ?: System.currentTimeMillis()
                    ),
                    isPrimaryGuardian = data["isPrimaryGuardian"] as? Boolean ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    // Xóa liên kết parent-student
    suspend fun deleteParentStudentLink(parentId: String, studentId: String): Boolean {
        return try {
            val docId = "${parentId}_${studentId}"
            parentStudentRef.document(docId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Lấy liên kết cụ thể
    suspend fun getParentStudentLink(parentId: String, studentId: String): ParentStudent? {
        return try {
            val docId = "${parentId}_${studentId}"
            val doc = parentStudentRef.document(docId).get().await()
            if (!doc.exists()) return null
            
            val data = doc.data ?: return null
            ParentStudent(
                parentId = data["parentId"] as? String ?: "",
                studentId = data["studentId"] as? String ?: "",
                relationship = RelationshipType.valueOf(
                    (data["relationship"] as? String) ?: "GUARDIAN"
                ),
                linkedAt = Instant.ofEpochMilli(
                    (data["linkedAt"] as? Long) ?: System.currentTimeMillis()
                ),
                isPrimaryGuardian = data["isPrimaryGuardian"] as? Boolean ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
}

