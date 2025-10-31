package com.example.datn.presentation.common.notifications

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor() {

    private val _state = MutableStateFlow(NotificationState())
    val state = _state.asStateFlow()

    // Hàng đợi các thông báo chờ hiển thị
    private val notificationQueue = ArrayDeque<NotificationEvent.Show>()

    fun onEvent(event: NotificationEvent) {
        when (event) {

            is NotificationEvent.Show -> {
                Log.d("NotificationManager", "Received Show event: ${event.message}")

                // Nếu đang hiển thị dialog câu hỏi thì thêm vào hàng đợi
                val current = _state.value
                if (current.isVisible) {
                    if (notificationQueue.size >= MAX_QUEUE_SIZE) {
                        val removed = notificationQueue.removeFirst()
                        Log.d("NotificationManager", "Queue full — removed oldest: ${removed.message}")
                    }
                    notificationQueue.addLast(event)
                    Log.d("NotificationManager", "Queued notification: ${event.message}")
                    return
                }

                // Hiển thị thông báo mới
                _state.value = NotificationState(
                    message = event.message,
                    type = event.type,
                    isVisible = true,
                    duration = event.duration
                )

                Log.d("NotificationManager", "Displayed notification: ${event.message}")
            }

            is NotificationEvent.Dismiss -> {
                val prevMessage = _state.value.message
                Log.d("NotificationManager", "Dismiss called for: $prevMessage")

                // Ẩn thông báo hiện tại
                _state.value = _state.value.copy(
                    isVisible = false
                )

                // Nếu có thông báo trong hàng đợi thì hiển thị tiếp
                if (notificationQueue.isNotEmpty()) {
                    val nextEvent = notificationQueue.removeFirst()
                    Log.d("NotificationManager", "Showing queued notification: ${nextEvent.message}")
                    onEvent(nextEvent)
                }
            }
        }
    }

    fun clearQueue() {
        notificationQueue.clear()
        Log.d("NotificationManager", "Cleared all queued notifications")
    }

    companion object {
        private const val MAX_QUEUE_SIZE = 5
    }
}
