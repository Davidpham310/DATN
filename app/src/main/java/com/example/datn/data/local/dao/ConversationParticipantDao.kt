package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.ConversationParticipantEntity
import com.example.datn.data.local.entities.UserEntity

@Dao
interface ConversationParticipantDao : BaseDao<ConversationParticipantEntity> {

    /**
     * Lấy tất cả người tham gia của một cuộc hội thoại cụ thể.
     * Hữu ích cho việc hiển thị danh sách thành viên trong chat nhóm.
     *
     * @param conversationId ID của cuộc hội thoại.
     * @return Danh sách các bản ghi ConversationParticipantEntity.
     */
    @Query("SELECT * FROM conversation_participant WHERE conversationId = :conversationId")
    suspend fun getParticipantsByConversation(conversationId: String): List<ConversationParticipantEntity>

    /**
     * Lấy thông tin người tham gia của một người dùng cụ thể trong một hội thoại.
     * Thường dùng để kiểm tra cài đặt cá nhân như isMuted hoặc lastViewedAt.
     *
     * @param conversationId ID của cuộc hội thoại.
     * @param userId ID của người dùng.
     * @return ConversationParticipantEntity hoặc null.
     */
    @Query("SELECT * FROM conversation_participant WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun getParticipantStatus(conversationId: String, userId: String): ConversationParticipantEntity?

    /**
     * Lấy tất cả các cuộc hội thoại mà một người dùng tham gia (chỉ lấy ID hội thoại).
     *
     * @param userId ID của người dùng.
     * @return Danh sách ID của các cuộc hội thoại.
     */
    @Query("SELECT conversationId FROM conversation_participant WHERE userId = :userId")
    suspend fun getConversationIdsByUserId(userId: String): List<String>

    /**
     * Cập nhật thời gian xem cuối cùng của người dùng trong cuộc hội thoại
     */
    @Query("UPDATE conversation_participant SET lastViewedAt = :lastViewedAt WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun updateLastViewed(conversationId: String, userId: String, lastViewedAt: java.time.Instant)

    /**
     * Bật/tắt thông báo cho cuộc hội thoại
     */
    @Query("UPDATE conversation_participant SET isMuted = :isMuted WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun updateMuteStatus(conversationId: String, userId: String, isMuted: Boolean)

    /**
     * Xóa người tham gia khỏi cuộc hội thoại
     */
    @Query("DELETE FROM conversation_participant WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun removeParticipant(conversationId: String, userId: String)

    /**
     * Xóa tất cả người tham gia của cuộc hội thoại
     */
    @Query("DELETE FROM conversation_participant WHERE conversationId = :conversationId")
    suspend fun removeAllParticipants(conversationId: String)

    /**
     * Kiểm tra người dùng có tham gia cuộc hội thoại không
     */
    @Query("SELECT EXISTS(SELECT 1 FROM conversation_participant WHERE conversationId = :conversationId AND userId = :userId)")
    suspend fun isUserInConversation(conversationId: String, userId: String): Boolean
}