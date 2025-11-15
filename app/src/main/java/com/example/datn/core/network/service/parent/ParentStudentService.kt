package com.example.datn.core.network.service.parent

import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.ParentStudent
import com.example.datn.domain.models.RelationshipType
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class ParentStudentService @Inject constructor() :
    BaseFirestoreService<ParentStudent>(collectionName = "parent_student", clazz = ParentStudent::class.java) {

    // Lấy danh sách học sinh của phụ huynh
    suspend fun getStudentsByParentId(parentId: String): List<ParentStudent> {
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

    // Lấy danh sách phụ huynh của học sinh
    suspend fun getParentsByStudentId(studentId: String): List<ParentStudent> {
        val snapshot = collectionRef
            .whereEqualTo("studentId", studentId)
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

    // Lấy quan hệ giữa phụ huynh và học sinh
    suspend fun getRelationship(parentId: String, studentId: String): ParentStudent? {
        val docId = "${parentId}_${studentId}"
        return getById(docId)
    }

    // Kiểm tra xem phụ huynh có liên kết với học sinh không
    suspend fun isLinked(parentId: String, studentId: String): Boolean {
        return getRelationship(parentId, studentId) != null
    }

    // Xóa liên kết giữa phụ huynh và học sinh
    suspend fun unlinkParentStudent(parentId: String, studentId: String) {
        val docId = "${parentId}_${studentId}"
        delete(docId)
    }

    // Tạo hoặc cập nhật liên kết giữa phụ huynh và học sinh
    suspend fun linkParentStudent(
        parentId: String,
        studentId: String,
        relationship: RelationshipType,
        isPrimaryGuardian: Boolean = true
    ) {
        val docId = "${parentId}_${studentId}"
        val link = ParentStudent(
            parentId = parentId,
            studentId = studentId,
            relationship = relationship,
            linkedAt = Instant.now(),
            isPrimaryGuardian = isPrimaryGuardian
        )
        add(docId, link)
    }
}
