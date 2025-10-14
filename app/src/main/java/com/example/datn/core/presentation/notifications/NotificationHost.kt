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
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotificationHost(
    notificationManager: NotificationManager,
    modifier: Modifier = Modifier
) {
    val state by notificationManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    if (state.isVisible) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (state.isQuestion) {
                    // 🔹 Dạng câu hỏi với 2 nút
                    Column(
                        modifier = Modifier
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = {
                                state.onConfirm?.invoke()
                                notificationManager.onEvent(NotificationEvent.Dismiss)
                            }) { Text("OK") }
                            Button(onClick = {
                                state.onCancel?.invoke()
                                notificationManager.onEvent(NotificationEvent.Dismiss)
                            }) { Text("Cancel") }
                        }
                    }
                } else {
                    // 🔹 Thông báo đơn giản ở giữa màn hình
                    LaunchedEffect(state.message) {
                        scope.launch {
                            delay(state.duration) // hiển thị trong duration
                            notificationManager.onEvent(NotificationEvent.Dismiss)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (state.type) {
                                    NotificationType.SUCCESS -> Color(0xFF4CAF50)
                                    NotificationType.ERROR -> Color(0xFFF44336)
                                    NotificationType.INFO -> Color(0xFF2196F3)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(text = state.message, color = Color.White)
                    }
                }
            }
        }
    }
}

