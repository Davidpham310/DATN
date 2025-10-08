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

    // Lưu ý: Đối với việc lấy danh sách tên người tham gia trong hội thoại 1-1,
    // bạn sẽ cần một truy vấn JOIN phức tạp tương tự như trong ConversationDao
    // hoặc một hàm riêng để JOIN với bảng UserEntity.
}