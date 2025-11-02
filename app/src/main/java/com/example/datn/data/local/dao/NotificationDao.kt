package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.NotificationEntity
import com.example.datn.domain.models.NotificationType

@Dao
interface NotificationDao : BaseDao<NotificationEntity> {
    
    // Lấy notification theo ID
    @Query("SELECT * FROM notification WHERE id = :notificationId LIMIT 1")
    suspend fun getById(notificationId: String): NotificationEntity?
    
    // Lấy tất cả notification của user (sắp xếp theo thời gian)
    @Query("SELECT * FROM notification WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getNotificationsByUserId(userId: String): List<NotificationEntity>
    
    // Lấy notification chưa đọc của user
    @Query("SELECT * FROM notification WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    suspend fun getUnreadNotifications(userId: String): List<NotificationEntity>
    
    // Lấy notification đã đọc của user
    @Query("SELECT * FROM notification WHERE userId = :userId AND isRead = 1 ORDER BY createdAt DESC")
    suspend fun getReadNotifications(userId: String): List<NotificationEntity>
    
    // Lấy notification theo loại
    @Query("SELECT * FROM notification WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    suspend fun getNotificationsByType(userId: String, type: NotificationType): List<NotificationEntity>
    
    // Đếm số lượng notification chưa đọc
    @Query("SELECT COUNT(id) FROM notification WHERE userId = :userId AND isRead = 0")
    suspend fun getUnreadNotificationCount(userId: String): Int
    
    // Đánh dấu tất cả notification là đã đọc
    @Query("UPDATE notification SET isRead = 1 WHERE userId = :userId AND isRead = 0")
    suspend fun markAllAsRead(userId: String): Int
    
    // Đánh dấu notification cụ thể là đã đọc
    @Query("UPDATE notification SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsReadById(notificationId: String): Int
    
    // Xóa tất cả notification của user
    @Query("DELETE FROM notification WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String): Int
    
    // Xóa notification theo ID
    @Query("DELETE FROM notification WHERE id = :notificationId")
    suspend fun deleteById(notificationId: String): Int
    
    // Xóa notification cũ (trước một thời điểm nhất định)
    @Query("DELETE FROM notification WHERE userId = :userId AND createdAt < :beforeTimestamp")
    suspend fun deleteOldNotifications(userId: String, beforeTimestamp: Long): Int
    
    // Xóa tất cả notification đã đọc của user
    @Query("DELETE FROM notification WHERE userId = :userId AND isRead = 1")
    suspend fun deleteReadNotifications(userId: String): Int
    
    // Lấy notification mới nhất của user
    @Query("SELECT * FROM notification WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLatestNotifications(userId: String, limit: Int = 10): List<NotificationEntity>
    
    // Kiểm tra xem notification có tồn tại không
    @Query("SELECT EXISTS(SELECT 1 FROM notification WHERE id = :notificationId)")
    suspend fun exists(notificationId: String): Boolean
    
    // Đếm tổng số notification của user
    @Query("SELECT COUNT(id) FROM notification WHERE userId = :userId")
    suspend fun getTotalCount(userId: String): Int
    
    // Lấy tất cả notification đã gửi bởi sender
    @Query("SELECT * FROM notification WHERE senderId = :senderId ORDER BY createdAt DESC")
    suspend fun getNotificationsBySenderId(senderId: String): List<NotificationEntity>
}