package com.example.datn.core.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


@Composable
fun NotificationHost(
    notificationManager: NotificationManager,
    modifier: Modifier = Modifier
) {
    val state by notificationManager.state.collectAsState()
    val backgroundColor = when (state.type) {
        NotificationType.SUCCESS -> Color(0xFF4CAF50)
        NotificationType.ERROR -> Color(0xFFF44336)
        NotificationType.INFO -> Color(0xFF2196F3)
    }

    LaunchedEffect(state){
        if (state.isVisible) {
            delay(state.duration)
            notificationManager.onEvent(NotificationEvent.Dismiss)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.isVisible) {
            Box(
                modifier = Modifier
                    .background(backgroundColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text = state.message, color = Color.White)
            }
        }
    }
}