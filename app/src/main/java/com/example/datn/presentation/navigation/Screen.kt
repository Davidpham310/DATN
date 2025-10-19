package com.example.datn.presentation.navigation

sealed class Screen(val route: String) {

    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Role selector
    object RoleSelector : Screen("role_selector")

    // Home cho từng tác nhân
    object TeacherHome : Screen("teacher/home")
    object TeacherClassManager : Screen("teacher/class_manager")
    object TeacherStudentManager : Screen("teacher/student_manager")
    object TeacherLessonManager : Screen("teacher/lesson_manager/{classId}/{className}") {
        fun createRoute(classId: String, className: String): String =
            "teacher/lesson_manager/$classId/$className"

        val routeWithArgs = "teacher/lesson_manager/{classId}/{className}"
    }
    object TeacherNotification : Screen("teacher/notification")

    object ParentHome : Screen("parent/home")
    object StudentHome : Screen("student/home")
}
