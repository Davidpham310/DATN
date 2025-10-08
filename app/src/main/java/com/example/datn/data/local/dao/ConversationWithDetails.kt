package com.example.datn.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.example.datn.data.local.entities.ConversationEntity
import com.example.datn.data.local.entities.ConversationParticipantEntity
import com.example.datn.data.local.entities.UserEntity
import java.time.Instant

// Data Class này dùng để chứa kết quả truy vấn JOIN
data class ConversationWithDetails(
    // 1. Thông tin từ bảng Conversation
    @Embedded val conversation: ConversationEntity,

    // 2. Thông tin của người dùng hiện tại trong hội thoại đó (dùng để lấy lastViewedAt)
    // Cần tạo truy vấn phức tạp để filter, nhưng trong Room Relations cơ bản, ta chỉ có thể lấy tất cả participants.
    // Tùy chọn A: Sử dụng POJO đơn giản hơn cho truy vấn phức tạp
    val participantUserId: String, // ID của người tham gia còn lại (cho chat 1-1)
    val participantName: String, // Tên của người tham gia còn lại
    val lastViewedAt: Instant,
    val unreadCount: Int // Số tin nhắn chưa đọc (cần truy vấn phụ)
)