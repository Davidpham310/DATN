package com.example.datn.core.network.service.parent

import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.*
import io.minio.messages.Progress
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ParentService @Inject constructor() :
    BaseFirestoreService<User>(collectionName = "users", clazz = User::class.java) {

    // Lấy danh sách học sinh của phụ huynh
    suspend fun getChildrenByParentId(parentId: String): List<User> {
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

    // Lấy tiến độ học tập của học sinh
    suspend fun getProgressByStudentId(studentId: String): List<Progress> {
        val progressRef = firestore.collection("progress")
        val snapshot = progressRef
            .whereEqualTo("studentId", studentId)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(Progress::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    // Lấy danh sách bài học đã học
    suspend fun getLessonHistory(studentId: String): List<Lesson> {
        val lessonRef = firestore.collection("lessons")
        val snapshot = lessonRef
            .whereArrayContains("completedBy", studentId)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(Lesson::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    // Lấy thông báo dành cho phụ huynh
    suspend fun getNotificationsForParent(parentId: String): List<Notification> {
        val notiRef = firestore.collection("notifications")
        val snapshot = notiRef
            .whereEqualTo("receiverId", parentId)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(Notification::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    // Lấy tin nhắn giữa phụ huynh và giáo viên
    suspend fun getMessagesWithTeacher(parentId: String, teacherId: String): List<Message> {
        val messageRef = firestore.collection("messages")
        val snapshot = messageRef
            .whereEqualTo("senderId", parentId)
            .whereEqualTo("receiverId", teacherId)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(Message::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    // Gửi tin nhắn từ phụ huynh đến giáo viên
    suspend fun sendMessageToTeacher(message: Message): Boolean {
        return try {
            firestore.collection("messages")
                .add(message)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}