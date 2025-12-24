package com.example.datn.presentation.student

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                            when (screen) {
                                Screen.StudentHome -> Icon(Icons.Default.Home, null)
                                Screen.StudentMyClasses -> Icon(Icons.Default.School, null)
                                Screen.StudentTestList -> Icon(Icons.Default.Assignment, null)
                                Screen.StudentConversations -> Icon(Icons.Default.Chat, null)
                                Screen.StudentNotifications -> Icon(Icons.Default.Notifications, null)
                                Screen.StudentAccount -> Icon(Icons.Default.Person, null)
                                else -> Icon(Icons.Default.Home, null)
                            }
                        },
                        label = {
                            Text(
                                when (screen) {
                                    Screen.StudentHome -> "Trang chủ"
                                    Screen.StudentMyClasses -> "Lớp học"
                                    Screen.StudentTestList -> "Kiểm tra"
                                    Screen.StudentConversations -> "Tin nhắn"
                                    Screen.StudentNotifications -> "Thông báo"
                                    Screen.StudentAccount -> "Tài khoản"
                                    else -> ""
                                }
                            )
                        }
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
