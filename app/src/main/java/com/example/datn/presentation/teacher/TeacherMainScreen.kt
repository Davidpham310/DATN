package com.example.datn.presentation.teacher

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.datn.presentation.navigation.Screen
import com.example.datn.presentation.navigation.TeacherNavGraph

@Composable
fun TeacherMainScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.TeacherHome,
        Screen.TeacherClassManager,
        Screen.TeacherMessages,
        Screen.TeacherNotification,
        Screen.TeacherAccount
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
                                popUpTo(Screen.TeacherHome.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.TeacherHome -> Icons.Default.Home
                                    Screen.TeacherClassManager -> Icons.Default.School
                                    Screen.TeacherMessages -> Icons.Default.Message
                                    Screen.TeacherNotification -> Icons.Default.Notifications
                                    Screen.TeacherAccount -> Icons.Default.Person
                                    else -> Icons.Default.Home
                                },
                                contentDescription = null
                            )
                        },
                        label = if (currentRoute == screen.route) {
                            {
                                Text(
                                    when (screen) {
                                        Screen.TeacherHome -> "Trang chủ"
                                        Screen.TeacherClassManager -> "Lớp học"
                                        Screen.TeacherMessages -> "Nhắn tin"
                                        Screen.TeacherNotification -> "Thông báo"
                                        Screen.TeacherAccount -> "Tài khoản"
                                        else -> ""
                                    }
                                )
                            }
                        } else null,
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { innerPadding ->
        TeacherNavGraph(
            navController = navController,
            onNavigateToLogin = onNavigateToLogin,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

