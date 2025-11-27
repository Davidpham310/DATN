package com.example.datn.presentation.parent

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        Screen.ParentAccount
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
                                popUpTo(Screen.ParentHome.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.ParentHome -> Icon(Icons.Default.Home, null)
                                Screen.ParentMyClasses -> Icon(Icons.Default.School, null)
                                Screen.ParentConversations -> Icon(Icons.Default.Chat, null)
                                Screen.ParentAccount -> Icon(Icons.Default.Person, null)
                                else -> Icon(Icons.Default.Home, null)
                            }
                        },
                        label = {
                            Text(
                                when (screen) {
                                    Screen.ParentHome -> "Trang chủ"
                                    Screen.ParentMyClasses -> "Lớp học"
                                    Screen.ParentConversations -> "Tin nhắn"
                                    Screen.ParentAccount -> "Tài khoản"
                                    else -> ""
                                }
                            )
                        }
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
