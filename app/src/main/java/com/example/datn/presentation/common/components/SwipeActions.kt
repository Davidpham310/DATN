package com.example.datn.presentation.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Swipeable conversation item with actions
 * Swipe right: Mark as read
 * Swipe left: Mute/Unmute (future)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableConversationItem(
    onMarkAsRead: () -> Unit,
    onMuteToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left - Mute/Unmute
                    onMuteToggle?.invoke()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right - Mark as read
                    onMarkAsRead()
                    false // Don't actually dismiss, just trigger action
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            SwipeBackground(
                dismissState = dismissState,
                onMarkAsRead = onMarkAsRead,
                onMuteToggle = onMuteToggle
            )
        },
        content = {
            content()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(
    dismissState: SwipeToDismissBoxState,
    onMarkAsRead: () -> Unit,
    onMuteToggle: (() -> Unit)?
) {
    val direction = dismissState.dismissDirection ?: return

    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer
        SwipeToDismissBoxValue.Settled -> return
    }

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.DoneAll
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.VolumeOff
        SwipeToDismissBoxValue.Settled -> return
    }

    val text = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> "Đánh dấu đã đọc"
        SwipeToDismissBoxValue.EndToStart -> "Tắt thông báo"
        SwipeToDismissBoxValue.Settled -> return
    }

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Simple swipe background with icon and color
 */
@Composable
fun SwipeActionBackground(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    alignment: Alignment
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor
            )
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
