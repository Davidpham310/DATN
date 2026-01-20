package com.example.datn.presentation.common.notifications

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable
fun NotificationHost(
    notificationManager: NotificationManager
) {
    val state by notificationManager.state.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    val (backgroundColor, contentColor) = when (state.type) {
        NotificationType.SUCCESS -> colorScheme.primary to colorScheme.onPrimary
        NotificationType.ERROR -> colorScheme.error to colorScheme.onError
        NotificationType.INFO -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
    }

    LaunchedEffect(state) {
        if (state.isVisible && state.autoDismiss) {
            delay(state.duration)
            notificationManager.onEvent(NotificationEvent.Dismiss)
        }
    }

    if (state.isVisible) {
        Dialog(
            onDismissRequest = {
                notificationManager.onEvent(NotificationEvent.Dismiss)
            }
        ) {
            Surface(
                color = backgroundColor,
                contentColor = contentColor,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 14.dp
                    )
                )
            }
        }
        Log.e("NotificationHost", "Recomposing NotificationHost with state: $state")
        Log.e("NotificationHost", "Error : ${state.message}, Visible: ${state.isVisible}")
    }
}
