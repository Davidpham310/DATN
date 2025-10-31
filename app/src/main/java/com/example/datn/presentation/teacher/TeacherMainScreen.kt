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
fun TeacherMainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.TeacherHome,
        Screen.TeacherClassManager,
        Screen.TeacherMessages,
        Screen.TeacherNotification
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
                                popUpTo(Screen.TeacherHome.route){ inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.TeacherHome -> Icon(Icons.Default.Home, null)
                                Screen.TeacherClassManager -> Icon(Icons.Default.School, null)
                                Screen.TeacherMessages -> Icon(Icons.Default.Message, null)
                                Screen.TeacherTestManager -> Icon(Icons.Default.Assignment, null)
                                Screen.TeacherNotification -> Icon(Icons.Default.Notifications, null)
                                else -> Icon(Icons.Default.Home, null)
                            }
                        },
                        label = {
                            Text(
                                when (screen) {
                                    Screen.TeacherHome -> "Trang chủ"
                                    Screen.TeacherClassManager -> "Lớp học"
                                    Screen.TeacherMessages -> "Nhắn tin"
                                    Screen.TeacherTestManager -> "Bài kiểm tra"
                                    Screen.TeacherNotification -> "Thông báo"
                                    else -> ""
                                }
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        TeacherNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

