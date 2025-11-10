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
    
    // Enrollment Management
    object TeacherEnrollmentManagement : Screen("teacher/enrollment_management/{classId}/{className}") {
        fun createRoute(classId: String, className: String): String =
            "teacher/enrollment_management/$classId/$className"
        
        val routeWithArgs = "teacher/enrollment_management/{classId}/{className}"
    }
    
    // Class Members
    object TeacherClassMembers : Screen("teacher/class_members/{classId}/{className}") {
        fun createRoute(classId: String, className: String): String =
            "teacher/class_members/$classId/$className"
        
        val routeWithArgs = "teacher/class_members/{classId}/{className}"
    }
    
    // Student Detail
    object TeacherStudentDetail : Screen("teacher/student_detail/{studentId}/{classId}/{studentName}") {
        fun createRoute(studentId: String, classId: String, studentName: String): String =
            "teacher/student_detail/$studentId/$classId/${java.net.URLEncoder.encode(studentName, "UTF-8")}"
        
        val routeWithArgs = "teacher/student_detail/{studentId}/{classId}/{studentName}"
    }
    
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
    object TeacherSelectRecipient : Screen("teacher/select_recipient")
    object TeacherSelectGroupParticipants : Screen("teacher/select_group_participants")
    object TeacherChat : Screen("teacher/chat/{conversationId}/{recipientId}/{recipientName}") {
        fun createRoute(conversationId: String, recipientId: String, recipientName: String): String {
            val encodedName = java.net.URLEncoder.encode(recipientName, "UTF-8")
            return "teacher/chat/$conversationId/$recipientId/$encodedName"
        }

        val routeWithArgs = "teacher/chat/{conversationId}/{recipientId}/{recipientName}"
    }
    object TeacherGroupDetails : Screen("teacher/group_details/{conversationId}/{groupTitle}") {
        fun createRoute(conversationId: String, groupTitle: String): String {
            val encodedTitle = java.net.URLEncoder.encode(groupTitle, "UTF-8")
            return "teacher/group_details/$conversationId/$encodedTitle"
        }

        val routeWithArgs = "teacher/group_details/{conversationId}/{groupTitle}"
    }
    object TeacherAddMembersToGroup : Screen("teacher/add_members/{conversationId}") {
        fun createRoute(conversationId: String): String =
            "teacher/add_members/$conversationId"

        val routeWithArgs = "teacher/add_members/{conversationId}"
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
    object ParentClassList : Screen("parent/class_list")
    object ParentJoinClass : Screen("parent/join_class")
    object ParentConversations : Screen("parent/conversations")
    object ParentSelectTeacher : Screen("parent/select_teacher")
    object ParentSelectGroupParticipants : Screen("parent/select_group_participants")
    object ParentChat : Screen("parent/chat/{conversationId}/{recipientId}/{recipientName}") {
        fun createRoute(conversationId: String, recipientId: String, recipientName: String): String {
            val encodedName = java.net.URLEncoder.encode(recipientName, "UTF-8")
            return "parent/chat/$conversationId/$recipientId/$encodedName"
        }
        val routeWithArgs = "parent/chat/{conversationId}/{recipientId}/{recipientName}"
    }
    object ParentGroupDetails : Screen("parent/group_details/{conversationId}/{groupTitle}") {
        fun createRoute(conversationId: String, groupTitle: String): String {
            val encodedTitle = java.net.URLEncoder.encode(groupTitle, "UTF-8")
            return "parent/group_details/$conversationId/$encodedTitle"
        }

        val routeWithArgs = "parent/group_details/{conversationId}/{groupTitle}"
    }
    object ParentAddMembersToGroup : Screen("parent/add_members/{conversationId}") {
        fun createRoute(conversationId: String): String =
            "parent/add_members/$conversationId"

        val routeWithArgs = "parent/add_members/{conversationId}"
    }
    
    // Student routes
    object StudentHome : Screen("student/home")
    object StudentMyClasses : Screen("student/my_classes")
    object StudentJoinClass : Screen("student/join_class")
    object StudentClassDetail : Screen("student/class_detail/{classId}/{className}") {
        fun createRoute(classId: String, className: String): String {
            val encodedName = java.net.URLEncoder.encode(className, "UTF-8")
            return "student/class_detail/$classId/$encodedName"
        }
        val routeWithArgs = "student/class_detail/{classId}/{className}"
    }
    object StudentLessonView : Screen("student/lesson_view/{lessonId}/{lessonTitle}") {
        fun createRoute(lessonId: String, lessonTitle: String): String {
            val encodedTitle = java.net.URLEncoder.encode(lessonTitle, "UTF-8")
            return "student/lesson_view/$lessonId/$encodedTitle"
        }
        val routeWithArgs = "student/lesson_view/{lessonId}/{lessonTitle}"
    }
    object StudentAccount : Screen("student/account")
    object StudentConversations : Screen("student/conversations")
    object StudentSelectTeacher : Screen("student/select_teacher")
    object StudentSelectGroupParticipants : Screen("student/select_group_participants")
    object StudentChat : Screen("student/chat/{conversationId}/{recipientId}/{recipientName}") {
        fun createRoute(conversationId: String, recipientId: String, recipientName: String): String {
            val encodedName = java.net.URLEncoder.encode(recipientName, "UTF-8")
            return "student/chat/$conversationId/$recipientId/$encodedName"
        }
        val routeWithArgs = "student/chat/{conversationId}/{recipientId}/{recipientName}"
    }
    object StudentGroupDetails : Screen("student/group_details/{conversationId}/{groupTitle}") {
        fun createRoute(conversationId: String, groupTitle: String): String {
            val encodedTitle = java.net.URLEncoder.encode(groupTitle, "UTF-8")
            return "student/group_details/$conversationId/$encodedTitle"
        }

        val routeWithArgs = "student/group_details/{conversationId}/{groupTitle}"
    }
    object StudentAddMembersToGroup : Screen("student/add_members/{conversationId}") {
        fun createRoute(conversationId: String): String =
            "student/add_members/$conversationId"

        val routeWithArgs = "student/add_members/{conversationId}"
    }
    
    // Student Test System
    object StudentTestList : Screen("student/tests")
    object StudentTestTaking : Screen("student/test/{testId}/take") {
        fun createRoute(testId: String): String =
            "student/test/$testId/take"
        
        val routeWithArgs = "student/test/{testId}/take"
    }
    object StudentTestResult : Screen("student/test/{testId}/result/{resultId}") {
        fun createRoute(testId: String, resultId: String): String =
            "student/test/$testId/result/$resultId"
        
        val routeWithArgs = "student/test/{testId}/result/{resultId}"
    }
    
    // MiniGame Result
    object StudentMiniGameResult : Screen("student/minigame/{miniGameId}/result/{resultId}") {
        fun createRoute(miniGameId: String, resultId: String): String =
            "student/minigame/$miniGameId/result/$resultId"
        
        val routeWithArgs = "student/minigame/{miniGameId}/result/{resultId}"
    }
}
