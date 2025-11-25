package com.example.datn.presentation.common.utils

import com.example.datn.data.local.dao.ConversationWithListDetails
import com.example.datn.domain.models.ConversationType
import com.example.datn.domain.models.Message
import com.example.datn.core.utils.extensions.formatAsDate
import com.example.datn.core.utils.extensions.formatAsDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Utility functions for conversation UI
 * Makes integration easier
 */

/**
 * Get display name for conversation
 * - ONE_TO_ONE: participant name
 * - GROUP: group title
 */
fun ConversationWithListDetails.getDisplayName(): String {
    return when (conversationType) {
        ConversationType.ONE_TO_ONE -> participantName ?: "Người dùng"
        ConversationType.GROUP -> title ?: "Nhóm"
        else -> "Hội thoại"
    }
}

/**
 * Check if conversation has unread messages
 */
fun ConversationWithListDetails.hasUnread(): Boolean {
    return unreadCount > 0
}

/**
 * Check if conversation is muted
 */
fun ConversationWithListDetails.isMuted(): Boolean {
    return isMuted
}

/**
 * Format last message time for conversation list
 */
fun ConversationWithListDetails.getFormattedTime(): String {
    return formatConversationTime(lastMessageAt)
}

/**
 * Check if should show sender name in message bubble
 * Only in group chats and when sender changes
 */
fun shouldShowSenderName(
    currentMessage: Message,
    previousMessage: Message?,
    isGroupChat: Boolean
): Boolean {
    if (!isGroupChat) return false
    if (previousMessage == null) return true
    return currentMessage.senderId != previousMessage.senderId
}

/**
 * Get previous message in list
 */
fun getPreviousMessage(
    message: Message,
    messages: List<Message>
): Message? {
    val index = messages.indexOf(message)
    return if (index > 0) messages[index - 1] else null
}

/**
 * Group messages by date
 */
fun List<Message>.groupByDate(): Map<LocalDate, List<Message>> {
    return this.groupBy { it.sentAt.toLocalDate() }
        .toSortedMap(compareByDescending { it })
}

/**
 * Format timestamp for conversation list
 * - "14:30" for today
 * - "Hôm qua" for yesterday
 * - "Thứ Hai" for this week
 * - "04/11" for older
 */
private fun formatConversationTime(instant: Instant?): String {
    if (instant == null) return ""

    val now = Instant.now()
    val messageTime = instant.atZone(ZoneId.systemDefault())
    val diff = ChronoUnit.DAYS.between(
        messageTime.toLocalDate(), 
        now.atZone(ZoneId.systemDefault()).toLocalDate()
    )

    return when {
        diff == 0L -> {
            // Today - show time
            instant.formatAsDateTime("HH:mm")
        }
        diff == 1L -> {
            // Yesterday
            "Hôm qua"
        }
        diff < 7 -> {
            // This week - show day name
            instant.formatAsDateTime("EEEE")
        }
        else -> {
            // Older - show date
            instant.formatAsDateTime("dd/MM")
        }
    }
}

/**
 * Format timestamp for message bubble
 * Always shows HH:mm
 */
fun formatMessageTime(instant: Instant): String {
    return instant.formatAsDateTime("HH:mm")
}

/**
 * Format date for date header
 */
fun formatDateHeader(date: LocalDate): String {
    val today = LocalDate.now()
    val diff = ChronoUnit.DAYS.between(date, today)

    return when {
        diff == 0L -> "Hôm nay"
        diff == 1L -> "Hôm qua"
        diff < 7 -> {
            val dayName = date.format(DateTimeFormatter.ofPattern("EEEE"))
            val dateStr = date.formatAsDate("dd/MM")
            "$dayName, $dateStr"
        }
        date.year == today.year -> {
            date.formatAsDate("dd/MM")
        }
        else -> {
            date.formatAsDate("dd/MM/yyyy")
        }
    }
}

/**
 * Convert Instant to LocalDate
 */
fun Instant.toLocalDate(): LocalDate {
    return this.atZone(ZoneId.systemDefault()).toLocalDate()
}

/**
 * Check if message is from current user
 */
fun Message.isFromUser(userId: String): Boolean {
    return senderId == userId
}

/**
 * Get conversation type icon
 */
fun ConversationType.getIcon(): String {
    return when (this) {
        ConversationType.ONE_TO_ONE -> "👤"
        ConversationType.GROUP -> "👥"
        else -> "💬"
    }
}

/**
 * Get role color for badge
 * Can be used for Material colors
 */
enum class RoleColor {
    STUDENT,    // Primary
    PARENT,     // Secondary
    TEACHER     // Tertiary
}

/**
 * Validate message content
 */
fun String.isValidMessage(): Boolean {
    return this.isNotBlank() && this.trim().isNotEmpty()
}

/**
 * Truncate message for preview
 */
fun String.toPreview(maxLength: Int = 50): String {
    return if (this.length > maxLength) {
        "${this.take(maxLength)}..."
    } else {
        this
    }
}

/**
 * Format participant count for group chat
 */
fun Int.toParticipantCount(): String {
    return when {
        this == 1 -> "1 thành viên"
        this > 1 -> "$this thành viên"
        else -> "Không có thành viên"
    }
}

/**
 * Check if conversation is a group chat
 */
fun ConversationWithListDetails.isGroup(): Boolean {
    return conversationType == ConversationType.GROUP
}

/**
 * Check if conversation is one-to-one
 */
fun ConversationWithListDetails.isOneToOne(): Boolean {
    return conversationType == ConversationType.ONE_TO_ONE
}
