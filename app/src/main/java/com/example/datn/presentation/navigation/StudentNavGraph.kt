package com.example.datn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.datn.presentation.student.account.StudentAccountScreen
import com.example.datn.presentation.student.classmanager.JoinClassScreen
import com.example.datn.presentation.student.classmanager.MyClassesScreen
import com.example.datn.presentation.student.home.StudentHomeScreen
import com.example.datn.presentation.student.lessons.StudentClassDetailScreen
import com.example.datn.presentation.student.lessons.StudentLessonContentListScreen
import com.example.datn.presentation.student.lessons.StudentLessonViewScreen
import com.example.datn.presentation.student.messaging.SelectTeacherScreen
import com.example.datn.presentation.student.messaging.StudentSelectRecipientViewModel
import com.example.datn.presentation.common.messaging.ChatViewModel
import com.example.datn.presentation.common.messaging.ConversationListViewModel
import com.example.datn.presentation.common.messaging.screens.ChatScreen
import com.example.datn.presentation.common.messaging.screens.ConversationListScreen
import com.example.datn.presentation.common.messaging.screens.GroupDetailsScreen
import com.example.datn.presentation.common.messaging.screens.AddMembersToGroupScreen
import com.example.datn.presentation.common.messaging.screens.SelectGroupParticipantsScreen
import com.example.datn.presentation.student.tests.StudentTestListScreen
import com.example.datn.presentation.student.tests.StudentTestTakingScreen
import com.example.datn.presentation.student.tests.StudentTestResultScreen
import com.example.datn.presentation.student.games.MiniGameResultScreen
import com.example.datn.presentation.student.games.MiniGamePlayScreen
import com.example.datn.presentation.student.games.MiniGameListScreen

@Composable
fun StudentNavGraph(
    navController: NavHostController,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.StudentHome.route,
        modifier = modifier
    ) {
        composable(Screen.StudentHome.route) {
            StudentHomeScreen(
                onNavigateToMyClasses = {
                    navController.navigate(Screen.StudentMyClasses.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.StudentMyClasses.route) {
            MyClassesScreen(
                onNavigateToClassDetail = { classId, className ->
                    navController.navigate(
                        Screen.StudentClassDetail.createRoute(classId, className)
                    )
                },
                onNavigateToJoinClass = {
                    navController.navigate(Screen.StudentJoinClass.route)
                }
            )
        }
        
        composable(
            route = Screen.StudentClassDetail.routeWithArgs,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("className") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val encodedName = backStackEntry.arguments?.getString("className") ?: ""
            val className = try {
                java.net.URLDecoder.decode(encodedName, "UTF-8")
            } catch (e: Exception) {
                encodedName
            }
            
            StudentClassDetailScreen(
                classId = classId,
                className = className,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLesson = { lessonId, lessonTitle ->
                    navController.navigate(
                        Screen.StudentLessonContentList.createRoute(lessonId, lessonTitle)
                    )
                }
            )
        }
        
        composable(
            route = Screen.StudentLessonContentList.routeWithArgs,
            arguments = listOf(
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("lessonTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("lessonTitle") ?: ""
            val lessonTitle = try {
                java.net.URLDecoder.decode(encodedTitle, "UTF-8")
            } catch (e: Exception) {
                encodedTitle
            }

            StudentLessonContentListScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToContent = { contentId ->
                    navController.navigate(
                        Screen.StudentLessonView.createRoute(lessonId, contentId, lessonTitle)
                    )
                }
            )
        }

        composable(
            route = Screen.StudentLessonView.routeWithArgs,
            arguments = listOf(
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("contentId") { type = NavType.StringType },
                navArgument("lessonTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val contentId = backStackEntry.arguments?.getString("contentId") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("lessonTitle") ?: ""
            val lessonTitle = try {
                java.net.URLDecoder.decode(encodedTitle, "UTF-8")
            } catch (e: Exception) {
                encodedTitle
            }

            StudentLessonViewScreen(
                lessonId = lessonId,
                contentId = contentId,
                lessonTitle = lessonTitle,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.StudentJoinClass.route) {
            JoinClassScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.StudentAccount.route) {
            StudentAccountScreen(onNavigateToLogin = onNavigateToLogin)
        }
        
        composable(Screen.StudentConversations.route) {
            val viewModel: ConversationListViewModel = hiltViewModel()
            ConversationListScreen(
                viewModel = viewModel,
                onConversationClick = { conversationId, recipientId, recipientName ->
                    navController.navigate(
                        Screen.StudentChat.createRoute(conversationId, recipientId, recipientName)
                    )
                },
                onNewMessageClick = {
                    navController.navigate(Screen.StudentSelectTeacher.route)
                },
                onGroupChatClick = {
                    navController.navigate(Screen.StudentSelectGroupParticipants.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.StudentSelectTeacher.route) {
            com.example.datn.presentation.student.messaging.SelectTeacherScreen(
                onTeacherSelected = { teacherId, teacherName ->
                    // Debug log
                    android.util.Log.d("StudentNavGraph", "onTeacherSelected - teacherId: '$teacherId', teacherName: '$teacherName'")
                    
                    if (teacherId.isBlank()) {
                        android.util.Log.e("StudentNavGraph", "ERROR: teacherId is blank! Cannot navigate.")
                        return@SelectTeacherScreen
                    }
                    
                    val route = Screen.StudentChat.createRoute(
                        conversationId = "new",
                        recipientId = teacherId,
                        recipientName = teacherName
                    )
                    
                    android.util.Log.d("StudentNavGraph", "Navigating to route: $route")
                    navController.navigate(route)
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.StudentSelectGroupParticipants.route) {
            val selectViewModel: StudentSelectRecipientViewModel = hiltViewModel()
            val conversationViewModel: ConversationListViewModel = hiltViewModel()
            val conversationState by conversationViewModel.state.collectAsState()
            
            SelectGroupParticipantsScreen(
                selectRecipientViewModel = selectViewModel,
                conversationViewModel = conversationViewModel,
                onGroupCreated = { conversationId ->
                    // Sử dụng tên nhóm thực tế từ state
                    val groupTitle = conversationState.createdGroupTitle ?: "Nhóm"
                    navController.navigate(
                        Screen.StudentChat.createRoute(
                            conversationId = conversationId,
                            recipientId = "",
                            recipientName = groupTitle
                        )
                    ) {
                        popUpTo(Screen.StudentConversations.route)
                    }
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.StudentChat.routeWithArgs,
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
                android.util.Log.e("StudentNavGraph", "Error decoding recipientName: ${e.message}")
                encodedName
            }
            
            val viewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                conversationId = conversationId,
                recipientId = recipientId,
                recipientName = recipientName,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroupDetails = { convId, groupTitle ->
                    navController.navigate(
                        Screen.StudentGroupDetails.createRoute(convId, groupTitle)
                    )
                }
            )
        }
        
        composable(
            route = Screen.StudentGroupDetails.routeWithArgs,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("groupTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val groupTitle = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("groupTitle") ?: "", "UTF-8"
            )
            
            GroupDetailsScreen(
                conversationId = conversationId,
                groupTitle = groupTitle,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddMembers = { convId ->
                    navController.navigate(
                        Screen.StudentAddMembersToGroup.createRoute(convId)
                    )
                }
            )
        }
        
        composable(
            route = Screen.StudentAddMembersToGroup.routeWithArgs,
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
        
        // Test System Routes
        composable(Screen.StudentTestList.route) {
            StudentTestListScreen(
                onNavigateToTest = { testId ->
                    navController.navigate(
                        Screen.StudentTestTaking.createRoute(testId)
                    )
                },
                onNavigateToResult = { testId, resultId ->
                    navController.navigate(
                        Screen.StudentTestResult.createRoute(testId, resultId)
                    )
                }
            )
        }
        
        composable(
            route = Screen.StudentTestTaking.routeWithArgs,
            arguments = listOf(
                navArgument("testId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            
            StudentTestTakingScreen(
                testId = testId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResult = { testId, resultId ->
                    navController.navigate(
                        Screen.StudentTestResult.createRoute(testId, resultId)
                    ) {
                        popUpTo(Screen.StudentTestList.route)
                    }
                }
            )
        }
        
        composable(
            route = Screen.StudentTestResult.routeWithArgs,
            arguments = listOf(
                navArgument("testId") { type = NavType.StringType },
                navArgument("resultId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
            
            StudentTestResultScreen(
                testId = testId,
                resultId = resultId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // MiniGame Play Screen
        composable(
            route = Screen.StudentMiniGamePlay.routeWithArgs,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("lessonId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            
            MiniGamePlayScreen(
                miniGameId = gameId,
                onBack = { navController.popBackStack() },
                onGameComplete = { resultId ->
                    navController.navigate(
                        Screen.StudentMiniGameResult.createRoute(gameId, resultId)
                    )
                }
            )
        }
        
        // MiniGame Result Screen
        composable(
            route = Screen.StudentMiniGameResult.routeWithArgs,
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("resultId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            val resultId = backStackEntry.arguments?.getString("resultId") ?: ""
            
            MiniGameResultScreen(
                miniGameId = gameId,
                resultId = resultId,
                onNavigateBack = { navController.popBackStack() },
                onPlayAgain = {
                    navController.navigate(
                        Screen.StudentMiniGamePlay.createRoute(gameId)
                    )
                }
            )
        }
        
        // MiniGame List Screen - Lesson specific
        composable(
            route = Screen.StudentMiniGameList.routeWithArgs,
            arguments = listOf(
                navArgument("lessonId") { 
                    type = NavType.StringType
                },
                navArgument("lessonTitle") { 
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            val encodedTitle = backStackEntry.arguments?.getString("lessonTitle") ?: ""
            val lessonTitle = if (encodedTitle.isNotBlank()) {
                try {
                    java.net.URLDecoder.decode(encodedTitle, "UTF-8")
                } catch (e: Exception) {
                    encodedTitle
                }
            } else null
            
            MiniGameListScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { gameId ->
                    navController.navigate(
                        Screen.StudentMiniGamePlay.createRoute(gameId, lessonId ?: "")
                    )
                }
            )
        }
    }
}
