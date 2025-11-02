package com.example.datn.core.network.service.notification

import android.util.Log
import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.domain.models.Notification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
    private val client = OkHttpClient()
    
    // FCM Server Key - Trong production, key này phải được bảo vệ ở backend
    // TODO: Move this to backend server or use Cloud Functions
    private val FCM_SERVER_KEY = "BJ_uSSsQbOCVIP46XSYvDJm_XZ2KlPBngHrwbFmsh_iWHt8WQbMAd_k03-ooaXh3iAT3OoTCeHYrwAX2YTxiDnQ"
    private val FCM_API_URL = "https://fcm.googleapis.com/fcm/send"
    
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
        return try {
            Log.d(TAG, "Sending FCM notification to token: $token")
            
            // Tạo FCM payload
            val notification = JSONObject().apply {
                put("title", title)
                put("body", body)
            }
            
            val payload = JSONObject().apply {
                put("to", token)
                put("notification", notification)
                put("priority", "high")
                
                // Thêm data nếu có
                data?.let {
                    val dataJson = JSONObject()
                    it.forEach { (key, value) ->
                        dataJson.put(key, value)
                    }
                    put("data", dataJson)
                }
            }
            
            // Tạo HTTP request
            val requestBody = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val request = Request.Builder()
                .url(FCM_API_URL)
                .addHeader("Authorization", "key=$FCM_SERVER_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            // Gửi request
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            
            if (success) {
                Log.d(TAG, "FCM notification sent successfully")
            } else {
                Log.e(TAG, "Failed to send FCM notification: ${response.code} - ${response.message}")
                Log.e(TAG, "Response body: ${response.body?.string()}")
            }
            
            response.close()
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM notification", e)
            false
        }
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
        return try {
            Log.d(TAG, "Fetching notifications for user: $userId")
            
            val snapshot = collectionRef
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toEntity()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing notification document: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications for user: $userId", e)
            emptyList()
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
        return try {
            Log.d(TAG, "Counting unread notifications for user: $userId")
            
            val snapshot = collectionRef
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            snapshot.size()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error counting unread notifications", e)
            0
        }
    }
}
