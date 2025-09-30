package com.example.datn.core.presentation.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor() {
    private val _state = MutableStateFlow(NotificationState())
    val state = _state.asStateFlow()

    fun onEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.Show -> {
                _state.value = NotificationState(
                    message = event.message,
                    type = event.type,
                    isVisible = true,
                    duration = event.duration
                )
            }
            is NotificationEvent.Dismiss -> {
                _state.value = _state.value.copy(isVisible = false)
            }
        }
    }
}