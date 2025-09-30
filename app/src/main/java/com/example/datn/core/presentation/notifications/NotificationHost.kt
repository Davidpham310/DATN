package com.example.datn.core.presentation.notifications

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NotificationHost(notificationManager: NotificationManager) {
    val state = notificationManager.state.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isVisible, state.message) {
        if (state.isVisible && state.message.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = "Dismiss",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                delay(state.duration)
                notificationManager.onEvent(NotificationEvent.Dismiss)
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = when (state.type) {
                NotificationType.SUCCESS -> Color.Green
                NotificationType.ERROR -> Color.Red
                NotificationType.INFO -> Color.Gray
            },
            contentColor = Color.White
        )
    }
}