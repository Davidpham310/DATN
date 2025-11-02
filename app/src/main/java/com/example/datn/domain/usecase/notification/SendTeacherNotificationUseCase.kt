package com.example.datn.domain.usecase.notification

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Notification
import com.example.datn.domain.models.NotificationType
import com.example.datn.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Use case để gửi notification đến giáo viên
 * 
 * Chức năng:
 * - Tạo Notification object với các thông tin cần thiết
 * - Gửi notification qua FCM đến device của giáo viên
 * - Lưu notification vào Firestore để quản lý lịch sử
 * 
 * @param repository INotificationRepository để xử lý logic gửi và lưu notification
 */
class SendTeacherNotificationUseCase @Inject constructor(
    private val repository: INotificationRepository
) {
    /**
     * Gửi notification đến giáo viên
     * 
     * @param params SendTeacherNotificationParams chứa thông tin cần thiết
     * @return Flow<Resource<Unit>> trạng thái của quá trình gửi notification
     */
    operator fun invoke(params: SendTeacherNotificationParams): Flow<Resource<Unit>> {
        // Tạo Notification object
        val notification = Notification(
            id = params.notificationId ?: UUID.randomUUID().toString(),
            userId = params.teacherId,
            senderId = params.senderId,
            type = params.type,
            title = params.title,
            content = params.content,
            referenceObjectId = params.referenceObjectId,
            referenceObjectType = params.referenceObjectType,
            isRead = false,
            createdAt = Instant.now()
        )
        
        // Gửi notification qua repository
        return repository.sendNotificationToTeacher(
            teacherToken = params.teacherToken,
            notification = notification
        )
    }
}

/**
 * Data class chứa các tham số cần thiết để gửi notification
 * 
 * @param teacherId ID của giáo viên nhận notification
 * @param teacherToken FCM device token của giáo viên
 * @param senderId ID của người gửi notification (có thể null nếu là hệ thống)
 * @param type Loại notification (ASSIGNMENT, MESSAGE, SYSTEM_ALERT, etc.)
 * @param title Tiêu đề notification
 * @param content Nội dung notification
 * @param referenceObjectId ID của đối tượng liên quan (optional)
 * @param referenceObjectType Loại đối tượng liên quan (optional)
 * @param notificationId ID của notification (optional, sẽ tự động tạo nếu không có)
 */
data class SendTeacherNotificationParams(
    val teacherId: String,
    val teacherToken: String,
    val senderId: String?,
    val type: NotificationType,
    val title: String,
    val content: String,
    val referenceObjectId: String? = null,
    val referenceObjectType: String? = null,
    val notificationId: String? = null
)
