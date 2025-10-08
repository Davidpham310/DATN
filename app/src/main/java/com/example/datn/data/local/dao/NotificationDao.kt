package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.NotificationEntity

@Dao
interface NotificationDao : BaseDao<NotificationEntity> {
    @Query("SELECT * FROM notification WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getNotificationsByUserId(userId: String): List<NotificationEntity>

    @Query("SELECT COUNT(id) FROM notification WHERE userId = :userId AND isRead = 0")
    suspend fun getUnreadNotificationCount(userId: String): Int
}