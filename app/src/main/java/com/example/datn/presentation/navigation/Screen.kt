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
    object TeacherEnrollmentManagement : Screen("teacher/enrollment_management/{classId}/{className}") {
        fun createRoute(classId: String, className: String): String =
            "teacher/enrollment_management/$classId/$className"

        val routeWithArgs: String = "teacher/enrollment_management/{classId}/{className}"
    }

    object TeacherClassMembers : Screen("teacher/class_members/{classId}/{className}") {
        fun createRoute(classId: String, className: String): String =
            "teacher/class_members/$classId/$className"

        val routeWithArgs: String = "teacher/class_members/{classId}/{className}"
    }

    object TeacherStudentDetail :
        Screen("teacher/student_detail/{studentId}/{classId}/{studentName}") {

        fun createRoute(studentId: String, classId: String, studentName: String): String =
            "teacher/student_detail/$studentId/$classId/$studentName"

        val routeWithArgs: String =
            "teacher/student_detail/{studentId}/{classId}/{studentName}"
    }

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

    object TeacherSelectRecipient : Screen("teacher/select_recipient")

    object TeacherSelectGroupParticipants :
        Screen("teacher/select_group_participants")

    object TeacherGroupDetails :
        Screen("teacher/group_details/{conversationId}/{groupTitle}") {

        fun createRoute(conversationId: String, groupTitle: String): String =
            "teacher/group_details/$conversationId/$groupTitle"

        val routeWithArgs: String =
            "teacher/group_details/{conversationId}/{groupTitle}"
    }

    object TeacherAddMembersToGroup :
        Screen("teacher/add_members/{conversationId}") {

        fun createRoute(conversationId: String): String =
            "teacher/add_members/$conversationId"

        val routeWithArgs: String = "teacher/add_members/{conversationId}"
    }

    // Teacher Account
    object TeacherAccount : Screen("teacher/account")

    // Parent routes
    object ParentHome : Screen("parent/home")
    object ParentAccount : Screen("parent/account")
    object ParentConversations : Screen("parent/conversations")
    object ParentStudentDetail : Screen("parent/student_detail/{studentId}/{studentName}") {
        fun createRoute(studentId: String, studentName: String): String =
            "parent/student_detail/$studentId/$studentName"

        val routeWithArgs: String =
            "parent/student_detail/{studentId}/{studentName}"
    }

    // Student routes
    object StudentHome : Screen("student/home")
    object StudentMyClasses : Screen("student/my_classes")
    object StudentJoinClass : Screen("student/join_class")
    object StudentAccount : Screen("student/account")
    object StudentConversations : Screen("student/conversations")

    object StudentClassDetail : Screen("student/class_detail/{classId}/{className}") {
        fun createRoute(classId: String, className: String): String =
            "student/class_detail/$classId/$className"

        val routeWithArgs: String = "student/class_detail/{classId}/{className}"
    }

    object StudentLessonContentList :
        Screen("student/lesson_contents/{lessonId}/{lessonTitle}") {

        fun createRoute(lessonId: String, lessonTitle: String): String =
            "student/lesson_contents/$lessonId/$lessonTitle"

        val routeWithArgs: String =
            "student/lesson_contents/{lessonId}/{lessonTitle}"
    }

    object StudentLessonView :
        Screen("student/lesson_view/{lessonId}/{contentId}/{lessonTitle}") {

        fun createRoute(lessonId: String, contentId: String, lessonTitle: String): String =
            "student/lesson_view/$lessonId/$contentId/$lessonTitle"

        val routeWithArgs: String =
            "student/lesson_view/{lessonId}/{contentId}/{lessonTitle}"
    }

    object StudentChat :
        Screen("student/chat/{conversationId}/{recipientId}/{recipientName}") {

        fun createRoute(
            conversationId: String,
            recipientId: String,
            recipientName: String
        ): String = "student/chat/$conversationId/$recipientId/$recipientName"

        val routeWithArgs: String =
            "student/chat/{conversationId}/{recipientId}/{recipientName}"
    }

    object StudentSelectTeacher : Screen("student/select_teacher")

    object StudentSelectGroupParticipants :
        Screen("student/select_group_participants")

    object StudentGroupDetails :
        Screen("student/group_details/{conversationId}/{groupTitle}") {

        fun createRoute(conversationId: String, groupTitle: String): String =
            "student/group_details/$conversationId/$groupTitle"

        val routeWithArgs: String =
            "student/group_details/{conversationId}/{groupTitle}"
    }

    object StudentAddMembersToGroup :
        Screen("student/add_members/{conversationId}") {

        fun createRoute(conversationId: String): String =
            "student/add_members/$conversationId"

        val routeWithArgs: String = "student/add_members/{conversationId}"
    }

    object StudentTestList : Screen("student/test_list")

    object StudentTestTaking : Screen("student/test_taking/{testId}") {
        fun createRoute(testId: String): String =
            "student/test_taking/$testId"

        val routeWithArgs: String = "student/test_taking/{testId}"
    }

    object StudentTestResult :
        Screen("student/test_result/{testId}/{resultId}") {

        fun createRoute(testId: String, resultId: String): String =
            "student/test_result/$testId/$resultId"

        val routeWithArgs: String =
            "student/test_result/{testId}/{resultId}"
    }

    object StudentMiniGamePlay :
        Screen("student/minigame_play/{gameId}/{lessonId}") {

        fun createRoute(gameId: String, lessonId: String? = null): String {
            val safeLessonId = lessonId ?: ""
            return "student/minigame_play/$gameId/$safeLessonId"
        }

        val routeWithArgs: String =
            "student/minigame_play/{gameId}/{lessonId}"
    }

    object StudentMiniGameResult :
        Screen("student/minigame_result/{gameId}/{resultId}") {

        fun createRoute(gameId: String, resultId: String): String =
            "student/minigame_result/$gameId/$resultId"

        val routeWithArgs: String =
            "student/minigame_result/{gameId}/{resultId}"
    }

    object StudentMiniGameList :
        Screen("student/minigame_list/{lessonId}/{lessonTitle}") {

        fun createRoute(lessonId: String, lessonTitle: String): String =
            "student/minigame_list/$lessonId/$lessonTitle"

        val routeWithArgs: String =
            "student/minigame_list/{lessonId}/{lessonTitle}"
    }
}
