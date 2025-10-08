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
    object TeacherLessonManager : Screen("teacher/lesson_manager")
    object TeacherAssignmentManager : Screen("teacher/assignment_manager")
    object TeacherSubmissionReview : Screen("teacher/submission_review")
    object TeacherNotification : Screen("teacher/notification")
    object TeacherSchedule : Screen("teacher/schedule")

    object ParentHome : Screen("parent/home")
    object StudentHome : Screen("student/home")
}
