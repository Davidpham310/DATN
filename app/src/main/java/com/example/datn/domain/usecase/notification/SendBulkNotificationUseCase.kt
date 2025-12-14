package com.example.datn.domain.usecase.notification

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Notification
import com.example.datn.domain.models.NotificationType
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.repository.INotificationRepository
import com.example.datn.domain.repository.IUserRepository
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.models.EnrollmentStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * Use case để gửi notification cho nhiều người (bulk send)
 * 
 * Chức năng:
 * - Lấy danh sách users theo role/group
 * - Gửi notification cho tất cả users trong danh sách
 * - Xử lý lỗi cho từng user riêng biệt
 * - Trả về thống kê gửi thành công/thất bại
 */
class SendBulkNotificationUseCase @Inject constructor(
    private val notificationRepository: INotificationRepository,
    private val userRepository: IUserRepository,
    private val classRepository: IClassRepository,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "SendBulkNotificationUseCase"
    
    /**
     * Gửi notification cho nhiều người
     * 
     * @param params SendBulkNotificationParams
     * @return Flow<Resource<BulkSendResult>> kết quả gửi hàng loạt
     */
    operator fun invoke(params: SendBulkNotificationParams): Flow<Resource<BulkSendResult>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Starting bulk notification send for ${params.recipientType}")
            
            // 1. Lấy danh sách recipients theo type
            val recipients = getRecipients(params.recipientType, params.classId)
            
            if (recipients.isEmpty()) {
                emit(Resource.Error("Không tìm thấy người nhận nào"))
                return@flow
            }
            
            Log.d(TAG, "Found ${recipients.size} recipients")
            
            // 2. Gửi notification cho từng người
            var successCount = 0
            var failedCount = 0
            val failedUsers = mutableListOf<String>()
            
            recipients.forEach { user ->
                try {
                    // Tạo notification cho user này
                    val notification = Notification(
                        id = UUID.randomUUID().toString(),
                        userId = user.id,
                        senderId = params.senderId,
                        type = params.type,
                        title = params.title,
                        content = params.content,
                        referenceObjectId = params.referenceObjectId,
                        referenceObjectType = params.referenceObjectType,
                        isRead = false,
                        createdAt = Instant.now()
                    )
                    
                    // Gửi notification (Firestore only - giống nhắn tin)
                    val saveResult = notificationRepository.saveNotification(notification)
                        .dropWhile { it is Resource.Loading }
                        .first()
                    
                    if (saveResult is Resource.Success) {
                        successCount++
                        Log.d(TAG, "Sent to ${user.name} (${user.email}) successfully")
                    } else {
                        failedCount++
                        failedUsers.add(user.name)
                        Log.w(TAG, "Failed to send to ${user.name}: ${(saveResult as? Resource.Error)?.message}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    failedUsers.add(user.name)
                    Log.e(TAG, "Error sending to ${user.name}", e)
                }
            }
            
            // 3. Trả về kết quả
            val result = BulkSendResult(
                totalRecipients = recipients.size,
                successCount = successCount,
                failedCount = failedCount,
                failedUsers = failedUsers
            )
            
            Log.d(TAG, "Bulk send completed: $result")
            emit(Resource.Success(result))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in bulk notification send", e)
            emit(Resource.Error("Lỗi khi gửi thông báo hàng loạt: ${e.message}"))
        }
    }
    
    /**
     * Lấy danh sách recipients theo type
     */
    private suspend fun getRecipients(type: RecipientType, classId: String? = null): List<User> {
        return when (type) {
            RecipientType.ALL_TEACHERS -> {
                Log.d(TAG, "Querying users with role: ${UserRole.TEACHER.name}")
                val result = userRepository.getUsersByRole(UserRole.TEACHER.name)
                    .dropWhile { it is Resource.Loading }
                    .first() // Lấy Success hoặc Error đầu tiên sau Loading
                Log.d(TAG, "Query result type: ${result::class.simpleName}")
                
                when (result) {
                    is Resource.Success -> {
                        Log.d(TAG, "Success! Data: ${result.data}")
                        val users = result.data ?: emptyList()
                        Log.d(TAG, "Found ${users.size} teachers")
                        users.forEachIndexed { index, user ->
                            Log.d(TAG, "  Teacher[$index]: id=${user.id}, name=${user.name}, email=${user.email}")
                        }
                        users
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error getting teachers: ${result.message}")
                        emptyList()
                    }
                    is Resource.Loading -> {
                        Log.w(TAG, "Still loading...")
                        emptyList()
                    }
                }
            }
            RecipientType.ALL_PARENTS -> {
                Log.d(TAG, "Querying users with role: ${UserRole.PARENT.name}")
                val result = userRepository.getUsersByRole(UserRole.PARENT.name)
                    .dropWhile { it is Resource.Loading }
                    .first() // Lấy Success hoặc Error đầu tiên sau Loading
                
                Log.d(TAG, "Repository result type: ${result::class.simpleName}")
                when (result) {
                    is Resource.Success -> {
                        Log.d(TAG, "Success! Data: ${result.data}")
                        Log.d(TAG, "Users count: ${result.data?.size ?: 0}")
                        result.data?.forEachIndexed { index, user ->
                            Log.d(TAG, "  User[$index]: id=${user.id}, name=${user.name}, email=${user.email}, role=${user.role.name}")
                        }
                        val users = result.data ?: emptyList()
                        Log.d(TAG, "Returning ${users.size} parents")
                        users
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error getting parents: ${result.message}")
                        emptyList()
                    }
                    is Resource.Loading -> {
                        Log.w(TAG, "Still loading...")
                        emptyList()
                    }
                }
            }
            RecipientType.ALL_STUDENTS -> {
                Log.d(TAG, "Querying users with role: ${UserRole.STUDENT.name}")
                val result = userRepository.getUsersByRole(UserRole.STUDENT.name)
                    .dropWhile { it is Resource.Loading }
                    .first() // Lấy Success hoặc Error đầu tiên sau Loading
                
                when (result) {
                    is Resource.Success -> {
                        Log.d(TAG, "Success! Data: ${result.data}")
                        val users = result.data ?: emptyList()
                        Log.d(TAG, "Found ${users.size} students")
                        users.forEachIndexed { index, user ->
                            Log.d(TAG, "  Student[$index]: id=${user.id}, name=${user.name}, email=${user.email}")
                        }
                        users
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error getting students: ${result.message}")
                        emptyList()
                    }
                    is Resource.Loading -> {
                        Log.w(TAG, "Still loading...")
                        emptyList()
                    }
                }
            }
            RecipientType.STUDENTS_IN_CLASS -> {
                if (classId.isNullOrBlank()) {
                    Log.e(TAG, "classId is required for STUDENTS_IN_CLASS")
                    emptyList()
                } else {
                    Log.d(TAG, "Querying students in class: $classId")
                    val result = classRepository.getStudentsInClass(classId, EnrollmentStatus.APPROVED)
                        .dropWhile { it is Resource.Loading }
                        .first()
                    if (result is Resource.Error) {
                        Log.e(TAG, "Error getting students in class: ${result.message}")
                        emptyList()
                    } else {
                        val classStudents = (result as? Resource.Success)?.data ?: emptyList()
                        // Lấy User objects từ studentIds
                        val studentIds = classStudents.map { it.studentId }
                        val users = mutableListOf<User>()
                        studentIds.forEach { studentId ->
                            val userResult = userRepository.getUserById(studentId)
                                .dropWhile { it is Resource.Loading }
                                .first()
                            if (userResult is Resource.Success) {
                                userResult.data?.let { users.add(it) }
                            }
                        }
                        Log.d(TAG, "Found ${users.size} students in class")
                        users
                    }
                }
            }
            RecipientType.PARENTS_IN_CLASS -> {
                if (classId.isNullOrBlank()) {
                    Log.e(TAG, "classId is required for PARENTS_IN_CLASS")
                    emptyList()
                } else {
                    Log.d(TAG, "Querying parents of students in class: $classId")
                    val result = classRepository.getStudentsInClass(classId, EnrollmentStatus.APPROVED)
                        .dropWhile { it is Resource.Loading }
                        .first()
                    if (result is Resource.Error) {
                        Log.e(TAG, "Error getting students in class: ${result.message}")
                        emptyList()
                    } else {
                        val classStudents = (result as? Resource.Success)?.data ?: emptyList()
                        val studentIds = classStudents.map { it.studentId }
                        // Lấy parents từ students
                        val parentIds = mutableSetOf<String>()
                        studentIds.forEach { studentId ->
                            val userResult = userRepository.getUserById(studentId)
                                .dropWhile { it is Resource.Loading }
                                .first()
                            if (userResult is Resource.Success) {
                                val student = userResult.data
                                // Lấy parentId từ ParentStudent relation trong Firestore
                                try {
                                    val parentIdsFromFirestore = getParentIdsOfStudent(studentId)
                                    parentIds.addAll(parentIdsFromFirestore)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error getting parents of student $studentId: ${e.message}")
                                }
                            }
                        }
                        // Lấy User objects từ parentIds
                        val users = mutableListOf<User>()
                        parentIds.forEach { parentId ->
                            val userResult = userRepository.getUserById(parentId)
                                .dropWhile { it is Resource.Loading }
                                .first()
                            if (userResult is Resource.Success) {
                                userResult.data?.let { users.add(it) }
                            }
                        }
                        Log.d(TAG, "Found ${users.size} parents of students in class")
                        users
                    }
                }
            }
            RecipientType.ALL_USERS -> {
                Log.d(TAG, "Querying all users")
                val result = userRepository.getAllUsers()
                    .dropWhile { it is Resource.Loading }
                    .first()
                if (result is Resource.Error) {
                    Log.e(TAG, "Error getting all users: ${result.message}")
                }
                val users = (result as? Resource.Success)?.data ?: emptyList()
                Log.d(TAG, "Found ${users.size} total users")
                users
            }
            RecipientType.SPECIFIC_USER -> {
                // Sẽ không dùng bulk cho specific user
                emptyList()
            }
        }
    }
    
    /**
     * Lấy danh sách parent IDs của một student từ ParentStudent relation trong Firestore
     */
    private suspend fun getParentIdsOfStudent(studentId: String): List<String> {
        return try {
            val parentStudentRef = firestore.collection("parent_student")
            val snapshot = parentStudentRef.whereEqualTo("studentId", studentId).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.getString("parentId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting parents of student $studentId: ${e.message}")
            emptyList()
        }
    }
}

/**
 * Loại người nhận
 */
enum class RecipientType(val displayName: String, val requiresClass: Boolean = false) {
    SPECIFIC_USER("Người cụ thể", false),
    ALL_TEACHERS("Tất cả giáo viên", false),
    ALL_PARENTS("Tất cả phụ huynh", false),
    ALL_STUDENTS("Tất cả học sinh", false),
    STUDENTS_IN_CLASS("Học sinh trong lớp", true),
    PARENTS_IN_CLASS("Phụ huynh của học sinh trong lớp", true),
    ALL_USERS("Tất cả người dùng", false);
    
    companion object {
        fun fromDisplayName(name: String): RecipientType? {
            return values().find { it.displayName == name }
        }
    }
}

/**
 * Parameters cho bulk send
 */
data class SendBulkNotificationParams(
    val senderId: String,
    val recipientType: RecipientType,
    val type: NotificationType,
    val title: String,
    val content: String,
    val referenceObjectId: String? = null,
    val referenceObjectType: String? = null,
    val classId: String? = null // Required for STUDENTS_IN_CLASS and PARENTS_IN_CLASS
)

/**
 * Kết quả gửi hàng loạt
 */
data class BulkSendResult(
    val totalRecipients: Int,
    val successCount: Int,
    val failedCount: Int,
    val failedUsers: List<String>
) {
    val isFullySuccessful: Boolean
        get() = failedCount == 0
    
    val successRate: Float
        get() = if (totalRecipients > 0) successCount.toFloat() / totalRecipients else 0f
}
