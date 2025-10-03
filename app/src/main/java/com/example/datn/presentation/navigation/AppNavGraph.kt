package com.example.datn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.datn.presentation.auth.screens.ForgotPasswordScreen
import com.example.datn.presentation.auth.screens.LoginScreen
import com.example.datn.presentation.auth.screens.RegisterScreen
import com.example.datn.presentation.parent.home.ParentHomeScreen
import com.example.datn.presentation.student.home.StudentHomeScreen
import com.example.datn.presentation.teacher.home.TeacherHomeScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Login.route) {

        // Auth
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }


        // Teacher
        composable(Screen.TeacherHome.route) { TeacherHomeScreen() }

        // Parent
        composable(Screen.ParentHome.route) { ParentHomeScreen() }

        // Student
        composable(Screen.StudentHome.route) { StudentHomeScreen() }
    }
}
