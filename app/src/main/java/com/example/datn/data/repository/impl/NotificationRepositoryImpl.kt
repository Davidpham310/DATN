package com.example.datn.data.repository.impl

import android.util.Log
import com.example.datn.core.network.service.notification.FirestoreNotificationService
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.NotificationDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.Notification
import com.example.datn.domain.repository.INotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Implementation của NotificationRepository
 * 
 * Quản lý notifications với 2 nguồn dữ liệu:
 * - Local: Room database cho caching và offline support
 * - Remote: Firestore và FCM cho sync và push notifications
 */
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val firestoreService: FirestoreNotificationService
) : INotificationRepository {

    private val TAG = "NotificationRepositoryImpl"

    /**
     * Lấy danh sách notifications của user
     * Chiến lược: Lấy từ local cache trước, sau đó sync với remote
     */
    override fun getNotificationsForUser(userId: String): Flow<Resource<List<Notification>>> = callbackFlow {
        trySend(Resource.Loading())

        // 1) Emit local cache first (fast UI)
        try {
            val localNotifications = notificationDao.getNotificationsByUserId(userId)
            if (localNotifications.isNotEmpty()) {
                Log.d(TAG, "Loaded ${localNotifications.size} notifications from local cache")
                trySend(Resource.Success(localNotifications.map { it.toDomain() }))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load local notifications for user: $userId", e)
        }

        // 2) Listen realtime from Firestore (like messaging)
        val job = launch {
            firestoreService.listenNotificationsByUserId(userId).collectLatest { remoteNotifications ->
                Log.d(TAG, "Realtime notifications update for userId=$userId size=${remoteNotifications.size}")

                // Sync to local cache
                try {
                    remoteNotifications.forEach { notification ->
                        val entity = notification.toEntity()
                        if (notificationDao.exists(entity.id)) {
                            notificationDao.update(entity)
                        } else {
                            notificationDao.insert(entity)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed syncing notifications to local for user: $userId", e)
                }

                trySend(Resource.Success(remoteNotifications))
            }
        }

        awaitClose {
            job.cancel()
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getNotificationsForUser", e)
        emit(Resource.Error("Lỗi không xác định: ${e.message}"))
    }

    /**
     * Đánh dấu notification là đã đọc
     */
    override fun markAsRead(notificationId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Marking notification as read: $notificationId")
            
            // 1. Cập nhật trên Firestore
            firestoreService.markNotificationAsRead(notificationId)
            
            // 2. Cập nhật trên local database
            notificationDao.markAsReadById(notificationId)
            
            Log.d(TAG, "Notification marked as read successfully")
            emit(Resource.Success(Unit))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read: $notificationId", e)
            emit(Resource.Error("Không thể đánh dấu đã đọc: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in markAsRead", e)
        emit(Resource.Error("Lỗi không xác định: ${e.message}"))
    }

    /**
     * Lấy số lượng notification chưa đọc
     */
    override fun getUnreadCount(userId: String): Flow<Resource<Int>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Getting unread count for user: $userId")
            
            // Lấy từ local cache trước
            val localCount = notificationDao.getUnreadNotificationCount(userId)
            emit(Resource.Success(localCount))
            
            // Sau đó sync với remote
            val remoteCount = firestoreService.getUnreadCount(userId)
            Log.d(TAG, "Unread count from Firestore: $remoteCount")
            
            // Emit remote count nếu khác local
            if (remoteCount != localCount) {
                emit(Resource.Success(remoteCount))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count for user: $userId", e)
            
            // Fallback về local count
            try {
                val localCount = notificationDao.getUnreadNotificationCount(userId)
                emit(Resource.Success(localCount))
            } catch (localError: Exception) {
                emit(Resource.Error("Không thể đếm thông báo chưa đọc: ${e.message}"))
            }
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in getUnreadCount", e)
        emit(Resource.Error("Lỗi không xác định: ${e.message}"))
    }

    /**
     * Gửi notification đến giáo viên qua FCM và lưu vào Firestore
     * 
     * Flow:
     * 1. (FCM disabled on client) Lưu vào Firestore
     * 2. Lưu vào local database để tracking
     */
    override fun sendNotificationToTeacher(
        teacherToken: String,
        notification: Notification
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Sending notification to teacher (Firestore only). tokenIgnored=${teacherToken.isNotBlank()}")

            // 1. Lưu vào Firestore (quan trọng để có lịch sử)
            firestoreService.saveNotification(notification)
            Log.d(TAG, "Notification saved to Firestore")
            
            // 2. Lưu vào local database
            notificationDao.insert(notification.toEntity())
            Log.d(TAG, "Notification saved to local database")
            
            emit(Resource.Success(Unit))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification to teacher", e)
            emit(Resource.Error("Không thể gửi thông báo: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in sendNotificationToTeacher", e)
        emit(Resource.Error("Lỗi không xác định: ${e.message}"))
    }

    /**
     * Lưu notification vào Firestore
     */
    override fun saveNotification(notification: Notification): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Saving notification: ${notification.id}")
            
            // 1. Lưu vào Firestore
            val notificationId = firestoreService.saveNotification(notification)
            Log.d(TAG, "Notification saved to Firestore with ID: $notificationId")
            
            // 2. Lưu vào local database
            notificationDao.insert(notification.toEntity())
            Log.d(TAG, "Notification saved to local database")
            
            emit(Resource.Success(notificationId))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notification", e)
            emit(Resource.Error("Không thể lưu thông báo: ${e.message}"))
        }
    }.catch { e ->
        Log.e(TAG, "Flow error in saveNotification", e)
        emit(Resource.Error("Lỗi không xác định: ${e.message}"))
    }
}
