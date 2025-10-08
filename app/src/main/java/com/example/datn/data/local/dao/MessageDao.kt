package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.MessageEntity

@Dao
interface MessageDao : BaseDao<MessageEntity> {
    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY sentAt DESC LIMIT 50")
    suspend fun getMessagesByConversation(conversationId: String): List<MessageEntity>
}