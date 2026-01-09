package com.example.datn.data.remote.service.notification

import android.util.Log
import com.example.datn.data.remote.service.firestore.BaseFirestoreService
import com.example.datn.domain.models.Notification
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service để quản lý notification với Firebase Cloud Messaging và Firestore
 * 
 * Chức năng:
 * - Gửi notification qua FCM đến thiết bị của giáo viên
 * - Lưu notification vào Firestore để quản lý lịch sử
 * 
 * Note: Để gửi FCM từ client Android, cần sử dụng HTTP v1 API hoặc
 * Legacy API với server key. Trong production, nên sử dụng backend server
 * hoặc Cloud Functions để gửi FCM message an toàn hơn.
 */
@Singleton
class FirestoreNotificationService @Inject constructor() : 
    BaseFirestoreService<Notification>("notifications", Notification::class.java) {
    
    private val TAG = "FirestoreNotificationService"
    
    /**
     * Gửi notification đến bất kỳ user nào qua FCM
     * 
     * @param token FCM device token của user
     * @param title Tiêu đề notification
     * @param body Nội dung notification
     * @param data Dữ liệu bổ sung (optional)
     * @return true nếu gửi thành công, false nếu thất bại
     */
    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ): Boolean {
        return sendNotificationToTeacher(token, title, body, data)
    }
    
    /**
     * Gửi notification đến giáo viên qua FCM
     * 
     * @param token FCM device token của giáo viên
     * @param title Tiêu đề notification
     * @param body Nội dung notification
     * @param data Dữ liệu bổ sung (optional)
     * @return true nếu gửi thành công, false nếu thất bại
     */
    suspend fun sendNotificationToTeacher(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ): Boolean {
        Log.w(
            TAG,
            "FCM sending is disabled in client. Notification should be delivered via Firestore realtime like messaging. token=$token"
        )
        return false
    }
    
    /**
     * Lưu notification vào Firestore
     * 
     * @param notification Notification object cần lưu
     * @return ID của notification đã lưu
     */
    suspend fun saveNotification(notification: Notification): String {
        Log.d(TAG, "Saving notification to Firestore: ${notification.id}")
        
        return try {
            // Chuyển Notification thành Map để lưu vào Firestore
            val notificationMap = hashMapOf(
                "id" to notification.id,
                "userId" to notification.userId,
                "senderId" to notification.senderId,
                "type" to notification.type.name,
                "title" to notification.title,
                "content" to notification.content,
                "referenceObjectId" to notification.referenceObjectId,
                "referenceObjectType" to notification.referenceObjectType,
                "isRead" to notification.isRead,
                "createdAt" to notification.createdAt.toEpochMilli()
            )
            
            // Lưu vào Firestore với document ID là notification.id
            collectionRef.document(notification.id)
                .set(notificationMap)
                .await()
            
            Log.d(TAG, "Notification saved successfully with ID: ${notification.id}")
            notification.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notification to Firestore", e)
            throw e
        }
    }
    
    /**
     * Lấy tất cả notifications của một user
     * 
     * @param userId ID của user
     * @return List các notification của user
     */
    suspend fun getNotificationsByUserId(userId: String): List<Notification> {
        Log.d(TAG, "Fetching notifications for user: $userId")

        val snapshot = collectionRef
            .whereEqualTo("userId", userId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toEntity()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing notification document: ${doc.id}", e)
                null
            }
        }.sortedByDescending { it.createdAt }
    }

    suspend fun getNotificationsBySenderId(senderId: String): List<Notification> {
        Log.d(TAG, "Fetching notifications by senderId: $senderId")

        val snapshot = collectionRef
            .whereEqualTo("senderId", senderId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                doc.toEntity()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing notification document: ${doc.id}", e)
                null
            }
        }.sortedByDescending { it.createdAt }
    }

    fun listenNotificationsByUserId(userId: String): Flow<List<Notification>> = callbackFlow {
        var listener: ListenerRegistration? = null

        try {
            val query = collectionRef
                .whereEqualTo("userId", userId)

            listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toEntity()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing notification document: ${doc.id}", e)
                            null
                        }
                    }
                    trySend(notifications.sortedByDescending { it.createdAt })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up notifications listener for user: $userId", e)
            close(e)
        }

        awaitClose {
            listener?.remove()
        }
    }

    fun listenNotificationsBySenderId(senderId: String): Flow<List<Notification>> = callbackFlow {
        var listener: ListenerRegistration? = null

        try {
            val query = collectionRef
                .whereEqualTo("senderId", senderId)

            listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toEntity()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing notification document: ${doc.id}", e)
                            null
                        }
                    }
                    trySend(notifications.sortedByDescending { it.createdAt })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up notifications listener for sender: $senderId", e)
            close(e)
        }

        awaitClose {
            listener?.remove()
        }
    }
    
    /**
     * Đánh dấu notification là đã đọc
     * 
     * @param notificationId ID của notification
     */
    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            Log.d(TAG, "Marking notification as read: $notificationId")
            
            collectionRef.document(notificationId)
                .update("isRead", true)
                .await()
            
            Log.d(TAG, "Notification marked as read successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            throw e
        }
    }
    
    /**
     * Lấy số lượng notification chưa đọc của user
     * 
     * @param userId ID của user
     * @return Số lượng notification chưa đọc
     */
    suspend fun getUnreadCount(userId: String): Int {
        Log.d(TAG, "Counting unread notifications for user: $userId")

        val snapshot = collectionRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        return snapshot.size()
    }
}
