package com.example.datn.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.datn.core.presentation.notifications.NotificationEvent
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BaseViewModel cho toàn bộ ứng dụng.
 *
 * @param S State (phải kế thừa từ BaseState)
 * @param E Event (phải kế thừa từ BaseEvent)
 */
abstract class BaseViewModel<S : BaseState, E : BaseEvent>(
    initialState: S
) : ViewModel() {

    // State chung
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state

    @Inject
    lateinit var notificationManager: NotificationManager

    // Mỗi ViewModel sẽ xử lý event riêng
    abstract fun onEvent(event: E)

    // Hàm cập nhật state an toàn
    protected fun setState(reducer: S.() -> S) {
        _state.value = _state.value.reducer()
    }

    // Handler bắt lỗi toàn cục
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        showNotification(throwable.message ?: "Unexpected error", NotificationType.ERROR)
        onError(throwable.message ?: "Unexpected error")
    }

    // Launch coroutine an toàn
    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(errorHandler, block = block)
    }

    // Có thể override nếu từng ViewModel muốn xử lý lỗi riêng
    protected open fun onError(message: String) {
        showNotification(message, NotificationType.ERROR)
    }

    // Hàm bắn notification chung
    protected fun showNotification(
        message: String,
        type: NotificationType,
        duration: Long = 3000L
    ) {
        if (this::notificationManager.isInitialized) {
            notificationManager.onEvent(NotificationEvent.Show(message, type, duration))
        }
    }
}
