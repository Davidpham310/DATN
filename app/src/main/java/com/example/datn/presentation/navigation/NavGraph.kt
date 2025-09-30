package com.example.datn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.datn.presentation.auth.screens.LoginScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen() }
        // Thêm route khác: "student/home", "parent/home", etc.
        // Dựa trên role sau login, navigate tương ứng
    }
}