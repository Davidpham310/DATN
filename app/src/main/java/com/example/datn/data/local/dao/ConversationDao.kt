package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.ConversationEntity


@Dao
interface ConversationDao : BaseDao<ConversationEntity> {
    @Query("SELECT * FROM conversation WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    /**
     * Lấy danh sách tất cả các cuộc hội thoại mà một người dùng tham gia.
     * Sử dụng Subquery để lấy chi tiết người tham gia còn lại, tránh lỗi biên dịch JOIN.
     */
    @Query("""
    SELECT 
        C.id AS conversationId,
        C.type,
        C.lastMessageAt,
        C.title,
        CP.lastViewedAt AS lastViewedAt,
        
        -- WORKAROUND: Sử dụng Subquery để lấy ID người tham gia còn lại (thay thế cho U.id)
        (SELECT U.id 
         FROM user AS U
         JOIN conversation_participant AS Other_CP
         ON Other_CP.userId = U.id
         WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
         LIMIT 1) AS participantUserId,
        
        -- WORKAROUND: Sử dụng Subquery để lấy Tên người tham gia còn lại (thay thế cho U.name)
        (SELECT U.name 
         FROM user AS U
         JOIN conversation_participant AS Other_CP
         ON Other_CP.userId = U.id
         WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
         LIMIT 1) AS participantName,
        
        -- Truy vấn phụ đếm tin nhắn chưa đọc (Không cần thay đổi)
        (SELECT COUNT(M.id) 
         FROM message AS M 
         WHERE M.conversationId = C.id 
         AND M.sentAt > CP.lastViewedAt) AS unreadCount
         
    FROM conversation AS C
    
    -- Bảng liên kết 1: Lấy thông tin người dùng hiện tại (Giữ nguyên)
    INNER JOIN conversation_participant AS CP 
        ON C.id = CP.conversationId 
    WHERE CP.userId = :currentUserId 
    
    ORDER BY C.lastMessageAt DESC
""")
    suspend fun getConversationListForUser(currentUserId: String): List<ConversationWithListDetails>
}