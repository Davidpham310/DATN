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
    object TeacherLessonContentManager : Screen("teacher/lesson_content_manager/{lessonId}/{lessonTitle}") {
        fun createRoute(lessonId: String, lessonTitle: String): String =
            "teacher/lesson_content_manager/$lessonId/$lessonTitle"

        val routeWithArgs = "teacher/lesson_content_manager/{lessonId}/{lessonTitle}"
    }
    object TeacherLessonContentDetail :
        Screen("teacher/lesson_content_detail/{contentId}?url={contentUrl}") {

        fun createRoute(contentId: String, contentUrl: String): String =
            "teacher/lesson_content_detail/$contentId?url=$contentUrl"

        val routeWithArgs = "teacher/lesson_content_detail/{contentId}?url={contentUrl}"
    }
    object TeacherNotification : Screen("teacher/notification")
    
    // MiniGame routes trong context của LessonContent
    object TeacherLessonMiniGameManager : Screen("teacher/lesson_minigame_manager/{lessonId}/{lessonTitle}") {
        fun createRoute(lessonId: String, lessonTitle: String): String =
            "teacher/lesson_minigame_manager/$lessonId/$lessonTitle"

        val routeWithArgs = "teacher/lesson_minigame_manager/{lessonId}/{lessonTitle}"
    }
    object TeacherLessonMiniGameQuestionManager : Screen("teacher/lesson_minigame_question_manager/{gameId}/{gameTitle}") {
        fun createRoute(gameId: String, gameTitle: String): String =
            "teacher/lesson_minigame_question_manager/$gameId/$gameTitle"

        val routeWithArgs = "teacher/lesson_minigame_question_manager/{gameId}/{gameTitle}"
    }

    object ParentHome : Screen("parent/home")
    object StudentHome : Screen("student/home")
}
