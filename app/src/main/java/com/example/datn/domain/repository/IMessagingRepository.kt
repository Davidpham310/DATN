package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ConversationWithListDetails
import com.example.datn.domain.models.Conversation
import com.example.datn.domain.models.ConversationType
import com.example.datn.domain.models.Message
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface IMessagingRepository {
    // Lấy danh sách cuộc hội thoại
    fun getConversations(userId: String): Flow<Resource<List<ConversationWithListDetails>>>
    
    // Lấy tin nhắn
    fun getMessages(conversationId: String): Flow<Message>
    
    // Gửi tin nhắn
    fun sendMessage(senderId: String, recipientId: String, content: String, conversationId: String? = null): Flow<Resource<Unit>>
    
    // Đánh dấu đã đọc
    fun markConversationAsRead(conversationId: String, userId: String): Flow<Resource<Unit>>
    
    // Tạo cuộc hội thoại 1-1
    fun createOneToOneConversation(user1Id: String, user2Id: String): Flow<Resource<Conversation>>
    
    // Cập nhật cuộc hội thoại
    suspend fun updateLastMessageAt(conversationId: String, lastMessageAt: Instant): Resource<Unit>
    suspend fun updateConversationTitle(conversationId: String, title: String?): Resource<Unit>
    
    // Xóa cuộc hội thoại
    suspend fun deleteConversation(conversationId: String): Resource<Unit>
    
    // Tìm kiếm và lọc
    suspend fun searchConversations(userId: String, query: String): Resource<List<ConversationWithListDetails>>
    suspend fun getConversationsByType(userId: String, type: ConversationType): Resource<List<ConversationWithListDetails>>
    suspend fun getRecentConversations(userId: String, limit: Int): Resource<List<ConversationWithListDetails>>
    suspend fun getConversationsWithUnread(userId: String): Resource<List<ConversationWithListDetails>>
    
    // Thông tin thống kê
    suspend fun getConversationCount(userId: String): Resource<Int>
    suspend fun getTotalUnreadCount(userId: String): Resource<Int>
    suspend fun conversationExists(conversationId: String): Resource<Boolean>
    
    // Lấy chi tiết cuộc hội thoại
    suspend fun getConversationById(conversationId: String): Resource<Conversation?>
}