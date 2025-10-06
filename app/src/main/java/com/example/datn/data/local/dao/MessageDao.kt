package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) ORDER BY sentAt DESC")
    suspend fun getConversationForUser(userId: String): List<MessageEntity>
}