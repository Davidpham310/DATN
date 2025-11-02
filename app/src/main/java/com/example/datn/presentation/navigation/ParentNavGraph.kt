package com.example.datn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.datn.presentation.parent.account.ParentAccountScreen
import com.example.datn.presentation.parent.home.ParentHomeScreen

@Composable
fun ParentNavGraph(
    navController: NavHostController,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ParentHome.route,
        modifier = modifier
    ) {
        composable(Screen.ParentHome.route) {
            ParentHomeScreen()
        }

        composable(Screen.ParentAccount.route) {
            ParentAccountScreen(onNavigateToLogin = onNavigateToLogin)
        }
    }
}
