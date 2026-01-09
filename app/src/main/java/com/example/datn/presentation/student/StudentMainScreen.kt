package com.example.datn.presentation.student

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.datn.presentation.navigation.Screen
import com.example.datn.presentation.navigation.StudentNavGraph

@Composable
fun StudentMainScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.StudentHome,
        Screen.StudentMyClasses,
        Screen.StudentTestList,
        Screen.StudentConversations,
        Screen.StudentNotifications,
        Screen.StudentAccount
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.StudentHome.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.StudentHome -> Icons.Default.Home
                                    Screen.StudentMyClasses -> Icons.Default.School
                                    Screen.StudentTestList -> Icons.Default.Assignment
                                    Screen.StudentConversations -> Icons.Default.Chat
                                    Screen.StudentNotifications -> Icons.Default.Notifications
                                    Screen.StudentAccount -> Icons.Default.Person
                                    else -> Icons.Default.Home
                                },
                                contentDescription = null
                            )
                        },
                        label = if (currentRoute == screen.route) {
                            {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (screen) {
                                            Screen.StudentHome -> "Trang chủ"
                                            Screen.StudentMyClasses -> "Lớp học"
                                            Screen.StudentTestList -> "Kiểm tra"
                                            Screen.StudentConversations -> "Tin nhắn"
                                            Screen.StudentNotifications -> "Thông báo"
                                            Screen.StudentAccount -> "Tài khoản"
                                            else -> ""
                                        },
                                        textAlign = TextAlign.Center
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
        StudentNavGraph(
            navController = navController,
            onNavigateToLogin = onNavigateToLogin,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
