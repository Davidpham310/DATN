package com.example.datn.presentation.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.datn.data.local.dao.ConversationWithListDetails
import com.example.datn.domain.models.ConversationType
import com.example.datn.core.utils.extensions.formatAsDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Enhanced ConversationItem with:
 * - Unread badge
 * - Mute icon
 * - Better timestamp formatting
 * - Swipe actions support
 */
@Composable
fun EnhancedConversationItem(
    conversation: ConversationWithListDetails,
    onClick: () -> Unit,
    onMarkAsRead: (() -> Unit)? = null,
    onMuteToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        ConversationItemContent(
            conversation = conversation,
            onClick = onClick
        )
    }

    if (onMarkAsRead != null || onMuteToggle != null) {
        SwipeableConversationItem(
            onMarkAsRead = onMarkAsRead ?: {},
            onMuteToggle = onMuteToggle,
            modifier = modifier
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun ConversationItemContent(
    conversation: ConversationWithListDetails,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Conversation name
                Text(
                    text = getConversationName(conversation),
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Timestamp
                Text(
                    text = formatTimestamp(conversation.lastMessageAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingContent = {
            Text(
                text = conversation.lastMessage ?: "Chưa có tin nhắn",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.unreadCount > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
            )
        },
        leadingContent = {
            // Avatar or group icon
            ConversationAvatar(conversation)
        },
        trailingContent = {
            // Unread badge and mute icon
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Unread badge
                if (conversation.unreadCount > 0) {
                    UnreadBadge(count = conversation.unreadCount)
                }

                // Mute icon
                if (conversation.isMuted) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Đã tắt thông báo",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@Composable
private fun ConversationAvatar(conversation: ConversationWithListDetails) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (conversation.conversationType == ConversationType.GROUP) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (conversation.conversationType == ConversationType.GROUP) {
                    Icons.Default.Groups
                } else {
                    Icons.Default.Person
                },
                contentDescription = null,
                tint = if (conversation.conversationType == ConversationType.GROUP) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

/**
 * Get conversation display name
 * - ONE_TO_ONE: Hiển thị tên người nhắn
 * - GROUP: Hiển thị title nếu có, nếu không thì hiển thị tên 3 người đầu tiên
 */
private fun getConversationName(conversation: ConversationWithListDetails): String {
    // Debug logging
    android.util.Log.d("ConversationName", 
        "Type: ${conversation.conversationType}, " +
        "Title: '${conversation.title}', " +
        "ParticipantName: '${conversation.participantName}', " +
        "ParticipantNames: '${conversation.participantNames}'"
    )
    
    return when (conversation.conversationType) {
        ConversationType.ONE_TO_ONE -> {
            // Hiển thị tên người tham gia
            conversation.participantName ?: "Người dùng"
        }
        ConversationType.GROUP -> {
            // Ưu tiên hiển thị title nếu có
            if (!conversation.title.isNullOrBlank()) {
                conversation.title
            } else if (!conversation.participantNames.isNullOrBlank()) {
                // Nếu không có title, hiển thị tên 3 người đầu tiên
                val names = conversation.participantNames.split(", ").filter { it.isNotBlank() }
                if (names.isEmpty()) {
                    "Nhóm"
                } else if (names.size <= 3) {
                    names.joinToString(", ")
                } else {
                    "${names.take(3).joinToString(", ")}..."
                }
            } else {
                "Nhóm"
            }
        }
    }
}

/**
 * Unread badge component
 */
@Composable
private fun UnreadBadge(count: Int) {
    Badge(
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(instant: Instant?): String {
    if (instant == null) return ""

    val now = Instant.now()
    val messageTime = instant.atZone(ZoneId.systemDefault())
    val diff = ChronoUnit.DAYS.between(messageTime.toLocalDate(), now.atZone(ZoneId.systemDefault()).toLocalDate())

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
