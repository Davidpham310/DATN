package com.example.datn.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Role selector
    object RoleSelector : Screen("role_selector")

    // Home cho từng tác nhân
    object TeacherHome : Screen("teacher/home")
    object ParentHome : Screen("parent/home")
    object StudentHome : Screen("student/home")
}
