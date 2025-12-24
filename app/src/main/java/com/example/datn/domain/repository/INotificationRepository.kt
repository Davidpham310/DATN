package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Notification
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {
    fun getNotificationsForUser(userId: String): Flow<Resource<List<Notification>>>
    fun getNotificationsBySenderId(senderId: String): Flow<Resource<List<Notification>>>
    fun markAsRead(notificationId: String): Flow<Resource<Unit>>
    fun getUnreadCount(userId: String): Flow<Resource<Int>>
    
    // Gửi notification đến giáo viên qua FCM và lưu vào Firestore
    fun sendNotificationToTeacher(
        teacherToken: String,
        notification: Notification
    ): Flow<Resource<Unit>>
    
    // Lưu notification vào Firestore
    fun saveNotification(notification: Notification): Flow<Resource<String>>
}