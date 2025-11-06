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
import com.example.datn.presentation.student.messaging.SelectTeacherScreen
import com.example.datn.presentation.student.messaging.StudentSelectRecipientViewModel
import com.example.datn.presentation.common.messaging.ChatViewModel
import com.example.datn.presentation.common.messaging.ConversationListViewModel
import com.example.datn.presentation.common.messaging.screens.ChatScreen
import com.example.datn.presentation.common.messaging.screens.ConversationListScreen
import com.example.datn.presentation.common.messaging.screens.GroupDetailsScreen
import com.example.datn.presentation.common.messaging.screens.AddMembersToGroupScreen
import com.example.datn.presentation.common.messaging.screens.SelectGroupParticipantsScreen

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
                    // TODO: Navigate to class detail when implemented
                },
                onNavigateToJoinClass = {
                    navController.navigate(Screen.StudentJoinClass.route)
                }
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
    }
}
