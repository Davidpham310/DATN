package com.example.datn.data.remote.service.parent

import com.example.datn.data.remote.service.firestore.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.core.utils.mapper.internalToFirestoreMap
import com.example.datn.domain.models.ParentStudent
import com.example.datn.domain.models.RelationshipType
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class ParentStudentService @Inject constructor() :
    BaseFirestoreService<ParentStudent>(collectionName = "parent_student", clazz = ParentStudent::class.java) {

    // Override add để KHÔNG lưu field 'id' vào document parent_student,
    // chỉ dùng documentId = "parentId_studentId" làm khóa.
    override suspend fun add(id: String?, data: ParentStudent): String {
        val map = internalToFirestoreMap(data, clazz).toMutableMap()
        return if (id == null) {
            val docRef = collectionRef.add(map).await()
            docRef.id
        } else {
            collectionRef.document(id).set(map).await()
            id
        }
    }

    // Lấy danh sách học sinh của phụ huynh
    // parentId là ID từ collection parents (không phải userId)
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
    // parentId là ID từ collection parents (không phải userId)
    suspend fun getRelationship(parentId: String, studentId: String): ParentStudent? {
        // 1. Thử theo chuẩn mới: dùng documentId = "parentId_studentId"
        val docId = "${parentId}_${studentId}"
        val byId = getById(docId)
        if (byId != null) return byId

        // 2. Fallback cho dữ liệu cũ: query theo parentId + studentId, không phụ thuộc docId
        val snapshot = collectionRef
            .whereEqualTo("parentId", parentId)
            .whereEqualTo("studentId", studentId)
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
    // parentId là ID từ collection parents (không phải userId)
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
