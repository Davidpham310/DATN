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
    object TeacherLessonContentManager : Screen("teacher/lesson_content_manager/{classId}/{lessonId}/{lessonTitle}") {
        fun createRoute(classId: String, lessonId: String, lessonTitle: String): String =
            "teacher/lesson_content_manager/$classId/$lessonId/$lessonTitle"

        val routeWithArgs = "teacher/lesson_content_manager/{classId}/{lessonId}/{lessonTitle}"
    }
    object TeacherLessonContentDetail :
        Screen("teacher/lesson_content_detail/{contentId}?url={contentUrl}") {

        fun createRoute(contentId: String, contentUrl: String): String =
            "teacher/lesson_content_detail/$contentId?url=$contentUrl"

        val routeWithArgs = "teacher/lesson_content_detail/{contentId}?url={contentUrl}"
    }
    object TeacherNotification : Screen("teacher/notification")
    object TeacherSendNotification : Screen("teacher/send_notification")
    object TeacherTestManager : Screen("teacher/test_manager")
    
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

    object TeacherMiniGameOptionManager : Screen("teacher/minigame_option_manager/{questionId}/{questionContent}") {
        fun createRoute(questionId: String, questionContent: String): String =
            "teacher/minigame_option_manager/$questionId/$questionContent"

        val routeWithArgs = "teacher/minigame_option_manager/{questionId}/{questionContent}"
    }

    // Test routes trong context của LessonContent
    object TeacherLessonTestManager : Screen("teacher/lesson_test_manager/{classId}/{lessonId}/{lessonTitle}") {
        fun createRoute(classId: String, lessonId: String, lessonTitle: String): String =
            "teacher/lesson_test_manager/$classId/$lessonId/$lessonTitle"

        val routeWithArgs = "teacher/lesson_test_manager/{classId}/{lessonId}/{lessonTitle}"
    }

    object TeacherTestQuestionManager : Screen("teacher/test_question_manager/{testId}/{testTitle}") {
        fun createRoute(testId: String, testTitle: String): String =
            "teacher/test_question_manager/$testId/$testTitle"

        val routeWithArgs = "teacher/test_question_manager/{testId}/{testTitle}"
    }

    object TeacherTestOptionManager : Screen("teacher/test_option_manager/{questionId}/{questionContent}") {
        fun createRoute(questionId: String, questionContent: String): String =
            "teacher/test_option_manager/$questionId/$questionContent"

        val routeWithArgs = "teacher/test_option_manager/{questionId}/{questionContent}"
    }

    // Messaging routes
    object TeacherMessages : Screen("teacher/messages")
    object TeacherChat : Screen("teacher/chat/{conversationId}/{recipientId}/{recipientName}") {
        fun createRoute(conversationId: String, recipientId: String, recipientName: String): String =
            "teacher/chat/$conversationId/$recipientId/$recipientName"

        val routeWithArgs = "teacher/chat/{conversationId}/{recipientId}/{recipientName}"
    }
    
    // Teacher Account
    object TeacherAccount : Screen("teacher/account")

    // Parent routes
    object ParentHome : Screen("parent/home")
    object ParentAccount : Screen("parent/account")
    object ParentStudentManagement : Screen("parent/student_management")
    object ParentStudentDetail : Screen("parent/student_detail/{studentId}/{studentName}") {
        fun createRoute(studentId: String, studentName: String): String =
            "parent/student_detail/$studentId/$studentName"
        val routeWithArgs = "parent/student_detail/{studentId}/{studentName}"
    }
    object ParentConversations : Screen("parent/conversations")
    object ParentSelectTeacher : Screen("parent/select_teacher")
    object ParentChat : Screen("parent/chat/{conversationId}/{recipientId}/{recipientName}") {
        fun createRoute(conversationId: String, recipientId: String, recipientName: String): String {
            val encodedName = java.net.URLEncoder.encode(recipientName, "UTF-8")
            return "parent/chat/$conversationId/$recipientId/$encodedName"
        }
        val routeWithArgs = "parent/chat/{conversationId}/{recipientId}/{recipientName}"
    }
    
    // Student routes
    object StudentHome : Screen("student/home")
    object StudentMyClasses : Screen("student/my_classes")
    object StudentJoinClass : Screen("student/join_class")
    object StudentAccount : Screen("student/account")
    object StudentConversations : Screen("student/conversations")
    object StudentSelectTeacher : Screen("student/select_teacher")
    object StudentChat : Screen("student/chat/{conversationId}/{recipientId}/{recipientName}") {
        fun createRoute(conversationId: String, recipientId: String, recipientName: String): String {
            val encodedName = java.net.URLEncoder.encode(recipientName, "UTF-8")
            return "student/chat/$conversationId/$recipientId/$encodedName"
        }
        val routeWithArgs = "student/chat/{conversationId}/{recipientId}/{recipientName}"
    }
}
