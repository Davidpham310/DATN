package com.example.datn.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datn.core.presentation.notifications.NotificationEvent
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BaseViewModel cho toàn bộ ứng dụng.
 *
 * @param S State (phải kế thừa từ BaseState)
 * @param E Event (phải kế thừa từ BaseEvent)
 */
abstract class BaseViewModel<S : BaseState, E : BaseEvent>(
    initialState: S,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state

    private val _eventFlow = MutableSharedFlow<E>()
    val eventFlow: SharedFlow<E> = _eventFlow.asSharedFlow()

    // Mỗi ViewModel sẽ xử lý event riêng
    abstract fun onEvent(event: E)

    protected fun setState(reducer: S.() -> S) {
        _state.value = _state.value.reducer()
    }

    protected fun sendEvent(event: E) {
        viewModelScope.launch {
            _eventFlow.emit(event)
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        showNotification(throwable.message ?: "Unexpected error", NotificationType.ERROR)
        onError(throwable.message ?: "Unexpected error")
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(errorHandler, block = block)
    }

    protected open fun onError(message: String) {
        showNotification(message, NotificationType.ERROR)
    }

    protected fun showNotification(
        message: String,
        type: NotificationType,
        duration: Long = 3000L
    ) {
        notificationManager.onEvent(NotificationEvent.Show(message, type, duration))
    }
}

