package com.example.datn.data.local.dao

import com.example.datn.domain.models.ConversationType
import java.time.Instant

data class ConversationWithListDetails(
    val conversationId: String, // Khớp với C.id AS conversationId
    val type: ConversationType, // Khớp với C.type
    val title: String?, // Khớp với C.title
    val lastMessageAt: Instant, // Khớp với C.lastMessageAt
    val lastMessage: String? = null, // Nội dung tin nhắn cuối

    // Thông tin của người dùng hiện tại
    val lastViewedAt: Instant, // Khớp với CP.lastViewedAt
    val unreadCount: Int, // Khớp với truy vấn phụ
    val isMuted: Boolean = false, // Trạng thái tắt thông báo

    // Thông tin của người tham gia còn lại (nếu là chat 1-1)
    val participantUserId: String?, // Khớp với U.id AS participantUserId
    val participantName: String?, // Khớp với U.name AS participantName
    
    // Danh sách tên tất c�� participants (phân cách bởi dấu phẩy) - dùng cho group chat
    val participantNames: String? = null // Khớp với GROUP_CONCAT
) {
    // Alias for compatibility
    val conversationType: ConversationType get() = type
}