package com.example.datn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.datn.presentation.common.notifications.NotificationHost
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.core.theme.AppTheme
import com.example.datn.core.utils.network.NetworkChecker
import com.example.datn.domain.repository.IAuthRepository
import com.example.datn.presentation.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationManager: NotificationManager
    @Inject
    lateinit var networkChecker: NetworkChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkChecker.startMonitoring()
        setContent {
            AppTheme {
                val navController = rememberNavController()

                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(navController = navController)
                    NotificationHost(notificationManager = notificationManager)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        networkChecker.stopMonitoring()
    }
}

