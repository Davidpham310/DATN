package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Notification
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {
    fun getNotificationsForUser(userId: String): Flow<Resource<List<Notification>>>
    fun markAsRead(notificationId: String): Flow<Resource<Unit>>
    fun getUnreadCount(userId: String): Flow<Resource<Int>>
}