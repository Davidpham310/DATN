package com.example.datn.presentation.parent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.datn.presentation.navigation.Screen
import com.example.datn.presentation.navigation.ParentNavGraph

@Composable
fun ParentMainScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    val navController = rememberNavController()

    val items = listOf(
        Screen.ParentHome,
        Screen.ParentMyClasses,
        Screen.ParentConversations,
        Screen.ParentNotifications,
        Screen.ParentAccount
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    val selected = currentRoute == screen.route

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.ParentHome.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.ParentHome -> Icons.Default.Home
                                    Screen.ParentMyClasses -> Icons.Default.School
                                    Screen.ParentConversations -> Icons.Default.Chat
                                    Screen.ParentNotifications -> Icons.Default.Notifications
                                    Screen.ParentAccount -> Icons.Default.Person
                                    else -> Icons.Default.Home
                                },
                                contentDescription = null
                            )
                        },
                        label = if (selected) {
                            {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (screen) {
                                            Screen.ParentHome -> "Trang chủ"
                                            Screen.ParentMyClasses -> "Lớp học"
                                            Screen.ParentConversations -> "Tin nhắn"
                                            Screen.ParentNotifications -> "Thông báo"
                                            Screen.ParentAccount -> "Tài khoản"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        } else null,
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { innerPadding ->
        ParentNavGraph(
            navController = navController,
            onNavigateToLogin = onNavigateToLogin,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
