package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.ConversationEntity
import com.example.datn.domain.models.Conversation
import kotlinx.coroutines.flow.Flow


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

    /**
     * Cập nhật thời gian tin nhắn cuối cùng của cuộc hội thoại
     */
    @Query("UPDATE conversation SET lastMessageAt = :lastMessageAt, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateLastMessageAt(conversationId: String, lastMessageAt: Long, updatedAt: Long)

    /**
     * Cập nhật tiêu đề cuộc hội thoại
     */
    @Query("UPDATE conversation SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateConversationTitle(conversationId: String, title: String?, updatedAt: Long)

    /**
     * Xóa cuộc hội thoại theo ID
     */
    @Query("DELETE FROM conversation WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: String)

    /**
     * Tìm kiếm cuộc hội thoại theo tên người tham gia hoặc tiêu đề
     */
    @Query("""
        SELECT 
            C.id AS conversationId,
            C.type,
            C.lastMessageAt,
            C.title,
            CP.lastViewedAt AS lastViewedAt,
            
            (SELECT U.id 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantUserId,
            
            (SELECT U.name 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantName,
            
            (SELECT COUNT(M.id) 
             FROM message AS M 
             WHERE M.conversationId = C.id 
             AND M.sentAt > CP.lastViewedAt) AS unreadCount
             
        FROM conversation AS C
        INNER JOIN conversation_participant AS CP 
            ON C.id = CP.conversationId 
        WHERE CP.userId = :currentUserId 
        AND (
            C.title LIKE '%' || :searchQuery || '%'
            OR (SELECT U.name 
                FROM user AS U
                JOIN conversation_participant AS Other_CP
                ON Other_CP.userId = U.id
                WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
                LIMIT 1) LIKE '%' || :searchQuery || '%'
        )
        ORDER BY C.lastMessageAt DESC
    """)
    suspend fun searchConversations(currentUserId: String, searchQuery: String): List<ConversationWithListDetails>

    /**
     * Lấy danh sách cuộc hội thoại theo loại (DIRECT, GROUP)
     */
    @Query("""
        SELECT 
            C.id AS conversationId,
            C.type,
            C.lastMessageAt,
            C.title,
            CP.lastViewedAt AS lastViewedAt,
            
            (SELECT U.id 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantUserId,
            
            (SELECT U.name 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantName,
            
            (SELECT COUNT(M.id) 
             FROM message AS M 
             WHERE M.conversationId = C.id 
             AND M.sentAt > CP.lastViewedAt) AS unreadCount
             
        FROM conversation AS C
        INNER JOIN conversation_participant AS CP 
            ON C.id = CP.conversationId 
        WHERE CP.userId = :currentUserId 
        AND C.type = :conversationType
        ORDER BY C.lastMessageAt DESC
    """)
    suspend fun getConversationsByType(currentUserId: String, conversationType: String): List<ConversationWithListDetails>

    /**
     * Đếm tổng số cuộc hội thoại của người dùng
     */
    @Query("""
        SELECT COUNT(DISTINCT C.id)
        FROM conversation AS C
        INNER JOIN conversation_participant AS CP 
            ON C.id = CP.conversationId 
        WHERE CP.userId = :currentUserId
    """)
    suspend fun getConversationCount(currentUserId: String): Int

    /**
     * Đếm tổng số tin nhắn chưa đọc của người dùng
     */
    @Query("""
        SELECT COUNT(M.id)
        FROM message AS M
        INNER JOIN conversation_participant AS CP
            ON M.conversationId = CP.conversationId
        WHERE CP.userId = :currentUserId
        AND M.sentAt > CP.lastViewedAt
    """)
    suspend fun getTotalUnreadCount(currentUserId: String): Int

    /**
     * Kiểm tra xem cuộc hội thoại có tồn tại không
     */
    @Query("SELECT EXISTS(SELECT 1 FROM conversation WHERE id = :conversationId)")
    suspend fun conversationExists(conversationId: String): Boolean

    /**
     * Lấy danh sách cuộc hội thoại gần đây (giới hạn số lượng)
     */
    @Query("""
        SELECT 
            C.id AS conversationId,
            C.type,
            C.lastMessageAt,
            C.title,
            CP.lastViewedAt AS lastViewedAt,
            
            (SELECT U.id 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantUserId,
            
            (SELECT U.name 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantName,
            
            (SELECT COUNT(M.id) 
             FROM message AS M 
             WHERE M.conversationId = C.id 
             AND M.sentAt > CP.lastViewedAt) AS unreadCount
             
        FROM conversation AS C
        INNER JOIN conversation_participant AS CP 
            ON C.id = CP.conversationId 
        WHERE CP.userId = :currentUserId 
        ORDER BY C.lastMessageAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentConversations(currentUserId: String, limit: Int): List<ConversationWithListDetails>

    /**
     * Lấy danh sách cuộc hội thoại có tin nhắn chưa đọc
     */
    @Query("""
        SELECT 
            C.id AS conversationId,
            C.type,
            C.lastMessageAt,
            C.title,
            CP.lastViewedAt AS lastViewedAt,
            
            (SELECT U.id 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantUserId,
            
            (SELECT U.name 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantName,
            
            (SELECT COUNT(M.id) 
             FROM message AS M 
             WHERE M.conversationId = C.id 
             AND M.sentAt > CP.lastViewedAt) AS unreadCount
             
        FROM conversation AS C
        INNER JOIN conversation_participant AS CP 
            ON C.id = CP.conversationId 
        WHERE CP.userId = :currentUserId
        AND EXISTS (
            SELECT 1 FROM message AS M 
            WHERE M.conversationId = C.id 
            AND M.sentAt > CP.lastViewedAt
        )
        ORDER BY C.lastMessageAt DESC
    """)
    suspend fun getConversationsWithUnreadMessages(currentUserId: String): List<ConversationWithListDetails>

    /**
     * Xóa tất cả cuộc hội thoại
     */
    @Query("DELETE FROM conversation")
    suspend fun deleteAllConversations()

    /**
     * Lấy danh sách cuộc hội thoại với Flow để lắng nghe thay đổi
     */
    @Query("""
        SELECT 
            C.id AS conversationId,
            C.type,
            C.lastMessageAt,
            C.title,
            CP.lastViewedAt AS lastViewedAt,
            
            (SELECT U.id 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantUserId,
            
            (SELECT U.name 
             FROM user AS U
             JOIN conversation_participant AS Other_CP
             ON Other_CP.userId = U.id
             WHERE Other_CP.conversationId = C.id AND Other_CP.userId <> :currentUserId
             LIMIT 1) AS participantName,
            
            (SELECT COUNT(M.id) 
             FROM message AS M 
             WHERE M.conversationId = C.id 
             AND M.sentAt > CP.lastViewedAt) AS unreadCount
             
        FROM conversation AS C
        INNER JOIN conversation_participant AS CP 
            ON C.id = CP.conversationId 
        WHERE CP.userId = :currentUserId 
        ORDER BY C.lastMessageAt DESC
    """)
    fun getConversationsWithDetails(currentUserId: String): Flow<List<ConversationWithListDetails>>

    /**
     * Tìm cuộc hội thoại 1-1 giữa 2 người dùng
     */
    @Query("""
        SELECT C.*
        FROM conversation AS C
        WHERE C.type = 'ONE_TO_ONE'
        AND C.id IN (
            SELECT CP1.conversationId
            FROM conversation_participant AS CP1
            INNER JOIN conversation_participant AS CP2
            ON CP1.conversationId = CP2.conversationId
            WHERE CP1.userId = :user1Id 
            AND CP2.userId = :user2Id
            AND CP1.userId <> CP2.userId
        )
        LIMIT 1
    """)
    suspend fun findOneToOneConversation(user1Id: String, user2Id: String): ConversationEntity?
}