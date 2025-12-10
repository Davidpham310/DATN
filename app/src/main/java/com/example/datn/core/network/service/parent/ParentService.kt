package com.example.datn.core.network.service.parent

import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.*
import io.minio.messages.Progress
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ParentService @Inject constructor() :
    BaseFirestoreService<Parent>(collectionName = "parents", clazz = Parent::class.java) {

    // Override getById để tìm theo field 'id' thay vì Firestore document ID
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

    // Lấy danh sách học sinh của phụ huynh (từ users collection)
    suspend fun getChildrenByParentId(parentId: String): List<User> {
        val usersRef = firestore.collection("users")
        val snapshot = usersRef
            .whereEqualTo("parentId", parentId)
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(User::class.java)
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