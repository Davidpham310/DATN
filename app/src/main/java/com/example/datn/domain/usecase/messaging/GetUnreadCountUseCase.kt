package com.example.datn.domain.usecase.messaging

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IMessagingRepository
import javax.inject.Inject

/**
 * Use Case để tính số tin nhắn chưa đọc
 * Logic: Đếm messages có sentAt > lastViewedAt của user
 */
class GetUnreadCountUseCase @Inject constructor(
    private val messagingRepository: IMessagingRepository
) {
    /**
     * Lấy tổng số tin chưa đọc của user
     */
    suspend fun getTotalUnreadCount(userId: String): Resource<Int> {
        return try {
            messagingRepository.getTotalUnreadCount(userId)
        } catch (e: Exception) {
            Resource.Error("Không thể lấy số tin chưa đọc: ${e.message}")
        }
    }
    
    /**
     * Lấy số tin chưa đọc của một conversation cụ thể
     * TODO: Implement in repository
     */
    suspend fun getConversationUnreadCount(conversationId: String, userId: String): Resource<Int> {
        return try {
            // Logic: Count messages where sentAt > lastViewedAt AND senderId != userId
            Resource.Success(0) // Placeholder
        } catch (e: Exception) {
            Resource.Error("Không thể lấy số tin chưa đọc: ${e.message}")
        }
    }
}
