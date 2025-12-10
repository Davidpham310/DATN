package com.example.datn.presentation.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IAuthRepository
import com.example.datn.presentation.teacher.classes.screens.ClassManagerScreen
import com.example.datn.presentation.teacher.enrollment.EnrollmentManagementScreen
import com.example.datn.presentation.teacher.home.TeacherHomeScreen
import com.example.datn.presentation.teacher.lessons.LessonContentManagerViewModel
import com.example.datn.presentation.teacher.lessons.screens.LessonContentDetailScreen
import com.example.datn.presentation.teacher.lessons.screens.LessonContentManagerScreen
import com.example.datn.presentation.teacher.lessons.screens.LessonManagerScreen
import com.example.datn.presentation.teacher.minigame.screens.LessonMiniGameManagerScreen
import com.example.datn.presentation.teacher.minigame.screens.LessonMiniGameQuestionManagerScreen
import com.example.datn.presentation.teacher.minigame.screens.MiniGameOptionManagerScreen
import com.example.datn.presentation.teacher.test.screens.LessonTestManagerScreen
import com.example.datn.presentation.teacher.test.screens.TestQuestionManagerScreen
import com.example.datn.presentation.teacher.test.screens.TestOptionManagerScreen
import com.example.datn.presentation.teacher.account.TeacherAccountScreen
import com.example.datn.presentation.teacher.notification.TeacherNotificationScreen
import com.example.datn.presentation.common.messaging.ChatViewModel
import com.example.datn.presentation.common.messaging.ConversationListViewModel
import com.example.datn.presentation.common.messaging.screens.ChatScreen
import com.example.datn.presentation.common.messaging.screens.ConversationListScreen
import com.example.datn.presentation.common.messaging.screens.GroupDetailsScreen
import com.example.datn.presentation.common.messaging.screens.AddMembersToGroupScreen
import com.example.datn.presentation.common.messaging.screens.SelectGroupParticipantsScreen
import com.example.datn.presentation.teacher.messaging.SelectRecipientViewModel
import com.example.datn.presentation.common.profile.EditProfileScreen

@Composable
fun TeacherNavGraph(
    navController: NavHostController,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TeacherHome.route,
        modifier = modifier
    ) {
        composable(Screen.TeacherHome.route) { TeacherHomeScreen() }
        composable(Screen.TeacherClassManager.route) {
            ClassManagerScreen(
                onNavigateToLessonManager = { classId, className ->
                    navController.navigate(
                        Screen.TeacherLessonManager.createRoute(classId, className)
                    )
                },
                onNavigateToEnrollmentManagement = { classId, className ->
                    navController.navigate(
                        Screen.TeacherEnrollmentManagement.createRoute(classId, className)
                    )
                },
                onNavigateToClassMembers = { classId, className ->
                    navController.navigate(
                        Screen.TeacherClassMembers.createRoute(classId, className)
                    )
                }
            )
        }
        
        // Enrollment Management Screen
        composable(
            route = Screen.TeacherEnrollmentManagement.routeWithArgs,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("className") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val className = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("className") ?: "", "UTF-8"
            )
            EnrollmentManagementScreen(
                classId = classId,
                className = className,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Class Members - View approved students in class
        composable(
            route = Screen.TeacherClassMembers.routeWithArgs,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("className") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val className = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("className") ?: "", "UTF-8"
            )
            com.example.datn.presentation.teacher.classmembers.ClassMembersScreen(
                classId = classId,
                className = className,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStudentDetail = { studentId, studentName ->
                    navController.navigate(
                        Screen.TeacherStudentDetail.createRoute(studentId, classId, studentName)
                    )
                }
            )
        }
        
        // Student Detail - View comprehensive student information
        composable(
            route = Screen.TeacherStudentDetail.routeWithArgs,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
                navArgument("classId") { type = NavType.StringType },
                navArgument("studentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val studentName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("studentName") ?: "", "UTF-8"
            )
            com.example.datn.presentation.teacher.studentdetail.StudentDetailScreen(
                studentId = studentId,
                classId = classId,
                studentName = studentName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Removed standalone TeacherTestManager - tests are now managed per lesson
        composable(
            route = Screen.TeacherLessonManager.routeWithArgs,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("className") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val className = backStackEntry.arguments?.getString("className") ?: ""
            LessonManagerScreen(
                classId = classId,
                className = className,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToContentManager = { lessonId, lessonTitle ->
                    navController.navigate(
                        Screen.TeacherLessonContentManager.createRoute(classId, lessonId, lessonTitle)
                    )
                }
            )
        }
        composable(
            route = Screen.TeacherLessonContentManager.routeWithArgs,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("lessonTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val lessonTitle = backStackEntry.arguments?.getString("lessonTitle") ?: ""
            val viewModel: LessonContentManagerViewModel = hiltViewModel(backStackEntry)
            LessonContentManagerScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onNavigateToDetail = { contentId, contentUrl ->
                    navController.navigate(
                        Screen.TeacherLessonContentDetail.createRoute(contentId, contentUrl)
                    )
                },
                onNavigateToMiniGame = { lessonId, lessonTitle ->
                    navController.navigate(
                        Screen.TeacherLessonMiniGameManager.createRoute(lessonId, lessonTitle)
                    )
                },
                onNavigateToTest = { lessonId, lessonTitle ->
                    navController.navigate(
                        Screen.TeacherLessonTestManager.createRoute(classId, lessonId, lessonTitle)
                    )
                },
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
        composable(
            route = Screen.TeacherLessonContentDetail.routeWithArgs,
            arguments = listOf(
                navArgument("contentId") { type = NavType.StringType },
                navArgument("contentUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.TeacherLessonContentManager.routeWithArgs)
            }
            val viewModel: LessonContentManagerViewModel = hiltViewModel(parentEntry)

            val contentId = backStackEntry.arguments?.getString("contentId") ?: ""
            val contentUrl = backStackEntry.arguments?.getString("contentUrl") ?: ""

            Log.d("TeacherNavGraph", "🟢 Shared VM: contentId=$contentId | url=$contentUrl")
            LessonContentDetailScreen(
                contentId = contentId,
                contentUrl = contentUrl,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TeacherLessonMiniGameManager.routeWithArgs,
            arguments = listOf(
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("lessonTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val lessonTitle = backStackEntry.arguments?.getString("lessonTitle") ?: ""
            LessonMiniGameManagerScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onNavigateToQuestions = { gameId, gameTitle ->
                    navController.navigate(
                        Screen.TeacherLessonMiniGameQuestionManager.createRoute(gameId, gameTitle)
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TeacherLessonMiniGameQuestionManager.routeWithArgs,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("gameTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            val gameTitle = backStackEntry.arguments?.getString("gameTitle") ?: ""
            LessonMiniGameQuestionManagerScreen(
                gameId = gameId,
                gameTitle = gameTitle,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOptions = { questionId, content ->
                    navController.navigate(
                        Screen.TeacherMiniGameOptionManager.createRoute(questionId, content)
                    )
                }
            )
        }

        composable(
            route = Screen.TeacherMiniGameOptionManager.routeWithArgs,
            arguments = listOf(
                navArgument("questionId") { type = NavType.StringType },
                navArgument("questionContent") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
            val questionContent = backStackEntry.arguments?.getString("questionContent") ?: ""
            MiniGameOptionManagerScreen(
                questionId = questionId,
                questionContent = questionContent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ==================== TEST NAVIGATION ====================
        composable(
            route = Screen.TeacherLessonTestManager.routeWithArgs,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("lessonTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val lessonTitle = backStackEntry.arguments?.getString("lessonTitle") ?: ""
            LessonTestManagerScreen(
                classId = classId,
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onNavigateToQuestions = { testId, testTitle ->
                    navController.navigate(
                        Screen.TeacherTestQuestionManager.createRoute(testId, testTitle)
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TeacherTestQuestionManager.routeWithArgs,
            arguments = listOf(
                navArgument("testId") { type = NavType.StringType },
                navArgument("testTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            val testTitle = backStackEntry.arguments?.getString("testTitle") ?: ""
            TestQuestionManagerScreen(
                testId = testId,
                testTitle = testTitle,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOptions = { questionId, content ->
                    navController.navigate(
                        Screen.TeacherTestOptionManager.createRoute(questionId, content)
                    )
                }
            )
        }

        composable(
            route = Screen.TeacherTestOptionManager.routeWithArgs,
            arguments = listOf(
                navArgument("questionId") { type = NavType.StringType },
                navArgument("questionContent") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
            val questionContent = backStackEntry.arguments?.getString("questionContent") ?: ""
            TestOptionManagerScreen(
                questionId = questionId,
                questionContent = questionContent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ==================== MESSAGING NAVIGATION ====================
        composable(Screen.TeacherMessages.route) {
            ConversationListScreen(
                onConversationClick = { conversationId, recipientId, recipientName ->
                    navController.navigate(
                        Screen.TeacherChat.createRoute(conversationId, recipientId, recipientName)
                    )
                },
                onNewMessageClick = {
                    navController.navigate(Screen.TeacherSelectRecipient.route)
                },
                onGroupChatClick = {
                    navController.navigate(Screen.TeacherSelectGroupParticipants.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.TeacherSelectRecipient.route) {
            com.example.datn.presentation.teacher.messaging.SelectRecipientScreen(
                onRecipientSelected = { recipientId, recipientName ->
                    // Debug log
                    android.util.Log.d("TeacherNavGraph", "onRecipientSelected - recipientId: '$recipientId', recipientName: '$recipientName'")
                    
                    if (recipientId.isBlank()) {
                        android.util.Log.e("TeacherNavGraph", "ERROR: recipientId is blank! Cannot navigate.")
                        return@SelectRecipientScreen
                    }
                    
                    val route = Screen.TeacherChat.createRoute(
                        conversationId = "new",
                        recipientId = recipientId,
                        recipientName = recipientName
                    )
                    
                    android.util.Log.d("TeacherNavGraph", "Navigating to route: $route")
                    navController.navigate(route)
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TeacherSelectGroupParticipants.route) {
            val selectViewModel: SelectRecipientViewModel = hiltViewModel()
            val conversationViewModel: ConversationListViewModel = hiltViewModel()
            val conversationState by conversationViewModel.state.collectAsState()
            
            SelectGroupParticipantsScreen(
                selectRecipientViewModel = selectViewModel,
                conversationViewModel = conversationViewModel,
                onGroupCreated = { conversationId ->
                    // Sử dụng tên nhóm thực tế từ state thay vì hardcode "Nhóm"
                    val groupTitle = conversationState.createdGroupTitle ?: "Nhóm"
                    navController.navigate(
                        Screen.TeacherChat.createRoute(
                            conversationId = conversationId,
                            recipientId = "",
                            recipientName = groupTitle
                        )
                    ) {
                        popUpTo(Screen.TeacherMessages.route)
                    }
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.TeacherChat.routeWithArgs,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("recipientId") { type = NavType.StringType },
                navArgument("recipientName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val recipientId = backStackEntry.arguments?.getString("recipientId") ?: ""
            val encodedName = backStackEntry.arguments?.getString("recipientName") ?: ""
            val recipientName = try {
                java.net.URLDecoder.decode(encodedName, "UTF-8")
            } catch (e: Exception) {
                android.util.Log.e("TeacherNavGraph", "Error decoding recipientName: ${e.message}")
                encodedName
            }
            
            // Debug log
            android.util.Log.d("TeacherNavGraph", "ChatScreen arguments - conversationId: '$conversationId', recipientId: '$recipientId', recipientName: '$recipientName'")
            
            if (recipientId.isBlank()) {
                android.util.Log.e("TeacherNavGraph", "ERROR: recipientId is blank in ChatScreen!")
            }
            
            ChatScreen(
                conversationId = conversationId,
                recipientId = recipientId,
                recipientName = recipientName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroupDetails = { convId, groupTitle ->
                    navController.navigate(
                        Screen.TeacherGroupDetails.createRoute(convId, groupTitle)
                    )
                }
            )
        }

        composable(
            route = Screen.TeacherGroupDetails.routeWithArgs,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("groupTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("groupTitle") ?: ""
            val groupTitle = try {
                java.net.URLDecoder.decode(encodedTitle, "UTF-8")
            } catch (e: Exception) {
                encodedTitle
            }

            GroupDetailsScreen(
                conversationId = conversationId,
                groupTitle = groupTitle,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddMembers = { convId ->
                    navController.navigate(
                        Screen.TeacherAddMembersToGroup.createRoute(convId)
                    )
                }
            )
        }

        composable(
            route = Screen.TeacherAddMembersToGroup.routeWithArgs,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""

            AddMembersToGroupScreen(
                conversationId = conversationId,
                onMembersAdded = { navController.popBackStack() },
                onDismiss = { navController.popBackStack() }
            )
        }

        // ==================== NOTIFICATION NAVIGATION ====================
        composable(Screen.TeacherNotification.route) {
            TeacherNotificationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Account
        composable(Screen.TeacherAccount.route) {
            TeacherAccountScreen(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToChangePassword = {
                    navController.navigate(Screen.TeacherChangePassword.route)
                },
                onNavigateToEditProfile = { userId, role ->
                    navController.navigate(Screen.EditProfile.createRoute(userId, role))
                }
            )
        }

        composable(Screen.TeacherChangePassword.route) {
            com.example.datn.presentation.teacher.account.TeacherChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Edit Profile Screen
        composable(
            route = Screen.EditProfile.routeWithArgs,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val role = backStackEntry.arguments?.getString("role") ?: return@composable
            
            EditProfileScreen(
                userId = userId,
                role = role,
                onNavigateBack = { navController.popBackStack() }
            )
        }

    }
}
