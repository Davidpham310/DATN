package com.example.datn.presentation.teacher.notification.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.notification.GetSentNotificationsUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherNotificationListViewModel @Inject constructor(
    private val getSentNotifications: GetSentNotificationsUseCase,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<TeacherNotificationListState, TeacherNotificationListEvent>(
    TeacherNotificationListState(),
    notificationManager
) {

    private var observeJob: Job? = null

    init {
        onEvent(TeacherNotificationListEvent.Load)
    }

    override fun onEvent(event: TeacherNotificationListEvent) {
        when (event) {
            TeacherNotificationListEvent.Load -> observeNotificationsForCurrentUser()
            TeacherNotificationListEvent.Refresh -> observeNotificationsForCurrentUser()
        }
    }

    private fun observeNotificationsForCurrentUser() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            authUseCases.getCurrentUser().collectLatest { userResult ->
                when (userResult) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }

                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = userResult.message) }
                        showNotification(
                            userResult.message ?: "Không thể lấy thông tin người dùng",
                            com.example.datn.presentation.common.notifications.NotificationType.ERROR
                        )
                    }

                    is Resource.Success -> {
                        val user = userResult.data
                        if (user == null) {
                            setState { copy(isLoading = false, error = "Không thể lấy thông tin người dùng") }
                            return@collectLatest
                        }

                        setState { copy(userId = user.id) }

                        getSentNotifications(user.id).collectLatest { result ->
                            when (result) {
                                is Resource.Loading -> {
                                    setState { copy(isLoading = true, error = null) }
                                }

                                is Resource.Error -> {
                                    setState { copy(isLoading = false, error = result.message) }
                                    showNotification(
                                        result.message ?: "Không thể tải thông báo",
                                        com.example.datn.presentation.common.notifications.NotificationType.ERROR
                                    )
                                }

                                is Resource.Success -> {
                                    setState {
                                        copy(
                                            isLoading = false,
                                            error = null,
                                            notifications = result.data ?: emptyList()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
