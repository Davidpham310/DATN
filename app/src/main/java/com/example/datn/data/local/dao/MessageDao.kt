package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao : BaseDao<MessageEntity> {
    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY sentAt DESC LIMIT 50")
    suspend fun getMessagesByConversation(conversationId: String): List<MessageEntity>

    /**
     * Lấy tin nhắn với Flow để lắng nghe thay đổi
     */
    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY sentAt ASC")
    fun getMessagesFlow(conversationId: String): Flow<List<MessageEntity>>

    /**
     * Lấy tin nhắn cuối cùng của cuộc hội thoại
     */
    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY sentAt DESC LIMIT 1")
    suspend fun getLastMessage(conversationId: String): MessageEntity?

    /**
     * Đánh dấu tin nhắn là đã đọc
     */
    @Query("UPDATE message SET isRead = 1 WHERE conversationId = :conversationId AND recipientId = :userId")
    suspend fun markMessagesAsRead(conversationId: String, userId: String)

    /**
     * Đếm số tin nhắn chưa đọc trong cuộc hội thoại
     */
    @Query("SELECT COUNT(*) FROM message WHERE conversationId = :conversationId AND recipientId = :userId AND isRead = 0")
    suspend fun getUnreadCount(conversationId: String, userId: String): Int

    /**
     * Xóa tin nhắn theo ID cuộc hội thoại
     */
    @Query("DELETE FROM message WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: String)

    /**
     * Lấy tin nhắn theo ID
     */
    @Query("SELECT * FROM message WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
}