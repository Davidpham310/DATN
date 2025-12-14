package com.example.datn.presentation.common.notification

import android.util.Log
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.notification.GetNotificationsUseCase
import com.example.datn.domain.usecase.notification.MarkNotificationAsReadUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getNotifications: GetNotificationsUseCase,
    private val markNotificationAsRead: MarkNotificationAsReadUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<NotificationState, NotificationEvent>(NotificationState(), notificationManager) {

    init {
        Log.d("NotificationViewModel", "Initialized")
    }

    override fun onEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.LoadNotifications -> loadNotifications(event.userId)
            is NotificationEvent.MarkAsRead -> markAsRead(event.notificationId)
            is NotificationEvent.ClearMessages -> setState { copy(successMessage = null, error = null) }
        }
    }

    private fun loadNotifications(userId: String) {
        Log.d("NotificationViewModel", "🔹 Loading notifications for userId: $userId")
        launch {
            getNotifications(userId).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d("NotificationViewModel", "⏳ Loading notifications...")
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val notifications = result.data ?: emptyList()
                        Log.d("NotificationViewModel", "✅ Loaded ${notifications.size} notifications")
                        setState {
                            copy(
                                isLoading = false,
                                notifications = notifications,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        Log.e("NotificationViewModel", "❌ Error loading notifications: ${result.message}")
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Lỗi tải thông báo", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun markAsRead(notificationId: String) {
        Log.d("NotificationViewModel", "Marking notification as read: $notificationId")

        // Optimistic UI update
        setState {
            copy(
                notifications = notifications.map { n ->
                    if (n.id == notificationId) n.copy(isRead = true) else n
                }
            )
        }

        launch {
            markNotificationAsRead(notificationId).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        // No UI change needed
                    }
                    is Resource.Success -> {
                        // Already updated optimistically
                    }
                    is Resource.Error -> {
                        Log.e("NotificationViewModel", "Error marking as read: ${result.message}")
                        showNotification(
                            result.message ?: "Không thể đánh dấu đã đọc",
                            NotificationType.ERROR
                        )
                    }
                }
            }
        }
    }
}
