package com.example.datn.core.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.datn.presentation.common.notifications.NotificationEvent
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            showNoNetworkNotification()
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            dismissNetworkNotification()
        }
    }

    private var monitoringScope: CoroutineScope? = null
    private var notificationJob: Job? = null
    private var isOfflineNotificationVisible: Boolean = false

    fun startMonitoring() {
        // Đăng ký NetworkCallback
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        // Coroutine liên tục check mạng
        monitoringScope = CoroutineScope(Dispatchers.IO)
        monitoringScope?.launch {
            while (isActive) {
                if (!isNetworkAvailable()) {
                    showNoNetworkNotification()
                } else {
                    dismissNetworkNotification()
                }
                delay(3000L) // kiểm tra mỗi 3s
            }
        }
    }

    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        monitoringScope?.cancel()
        notificationJob?.cancel()
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoNetworkNotification() {
        // Không tạo nhiều job trùng nhau
        if (isOfflineNotificationVisible) return

        isOfflineNotificationVisible = true

        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.onEvent(
                NotificationEvent.Show(
                    message = "Không có mạng. Vui lòng bật Wi-Fi hoặc 4G",
                    type = NotificationType.ERROR,
                    duration = 3000L,
                    autoDismiss = false,
                )
            )
        }
    }

    private fun dismissNetworkNotification() {
        if (!isOfflineNotificationVisible) return

        isOfflineNotificationVisible = false
        notificationJob?.cancel()
        CoroutineScope(Dispatchers.Main).launch {
            notificationManager.onEvent(NotificationEvent.Dismiss)
        }
    }
}
