package com.example.datn.core.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.datn.core.presentation.notifications.NotificationEvent
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkChecker(
    private val context: Context,
    private val notificationManager: NotificationManager
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            CoroutineScope(Dispatchers.Main).launch {
                notificationManager.onEvent(
                    NotificationEvent.Show(
                        message = "Không có kết nối mạng. Vui lòng bật Wi-Fi hoặc dữ liệu di động.",
                        type = NotificationType.ERROR,
                        duration = 5000L
                    )
                )
            }
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            CoroutineScope(Dispatchers.Main).launch {
                notificationManager.onEvent(NotificationEvent.Dismiss)
            }
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            CoroutineScope(Dispatchers.Main).launch {
                notificationManager.onEvent(
                    NotificationEvent.Show(
                        message = "Không có kết nối mạng",
                        type = NotificationType.ERROR
                    )
                )
            }
        }
    }


    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
