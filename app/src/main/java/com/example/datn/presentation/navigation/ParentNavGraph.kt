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
import com.example.datn.presentation.parent.account.ParentAccountScreen
import com.example.datn.presentation.parent.home.ParentHomeScreen
import com.example.datn.presentation.parent.messaging.SelectTeacherScreen
import com.example.datn.presentation.parent.messaging.ParentSelectRecipientViewModel
import com.example.datn.presentation.parent.studentmanagement.ParentStudentManagementScreen
import com.example.datn.presentation.parent.studentprofile.StudentDetailScreen
import com.example.datn.presentation.parent.classlist.ParentClassListScreen
import com.example.datn.presentation.parent.classmanager.ParentJoinClassScreen
import com.example.datn.presentation.common.messaging.ChatViewModel
import com.example.datn.presentation.common.messaging.ConversationListViewModel
import com.example.datn.presentation.common.messaging.screens.ChatScreen
import com.example.datn.presentation.common.messaging.screens.ConversationListScreen
import com.example.datn.presentation.common.messaging.screens.GroupDetailsScreen
import com.example.datn.presentation.common.messaging.screens.AddMembersToGroupScreen
import com.example.datn.presentation.common.messaging.screens.SelectGroupParticipantsScreen

@Composable
fun ParentNavGraph(
    navController: NavHostController,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ParentHome.route,
        modifier = modifier
    ) {
        composable(Screen.ParentHome.route) {
            ParentHomeScreen(navController = navController)
        }

        composable(Screen.ParentAccount.route) {
            ParentAccountScreen(onNavigateToLogin = onNavigateToLogin)
        }
        
        composable(Screen.ParentStudentManagement.route) {
            ParentStudentManagementScreen(navController = navController)
        }
        
        composable(
            route = Screen.ParentStudentDetail.routeWithArgs,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
                navArgument("studentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val studentName = backStackEntry.arguments?.getString("studentName") ?: ""
            StudentDetailScreen(
                studentId = studentId,
                studentName = studentName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ParentClassList.route) {
            ParentClassListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClassDetail = { classId ->
                    // TODO: Navigate to class detail screen when implemented
                    // navController.navigate(Screen.ParentClassDetail.createRoute(classId))
                },
                onNavigateToJoinClass = {
                    navController.navigate(Screen.ParentJoinClass.route)
                }
            )
        }
        
        composable(Screen.ParentJoinClass.route) {
            ParentJoinClassScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ParentConversations.route) {
            val viewModel: ConversationListViewModel = hiltViewModel()
            ConversationListScreen(
                viewModel = viewModel,
                onConversationClick = { conversationId, recipientId, recipientName ->
                    navController.navigate(
                        Screen.ParentChat.createRoute(conversationId, recipientId, recipientName)
                    )
                },
                onNewMessageClick = {
                    navController.navigate(Screen.ParentSelectTeacher.route)
                },
                onGroupChatClick = {
                    navController.navigate(Screen.ParentSelectGroupParticipants.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ParentSelectTeacher.route) {
            SelectTeacherScreen(
                onTeacherSelected = { teacherId, teacherName ->
                    // Debug log
                    android.util.Log.d("ParentNavGraph", "onTeacherSelected - teacherId: '$teacherId', teacherName: '$teacherName'")
                    
                    if (teacherId.isBlank()) {
                        android.util.Log.e("ParentNavGraph", "ERROR: teacherId is blank! Cannot navigate.")
                        return@SelectTeacherScreen
                    }
                    
                    // Navigate to chat with new conversation
                    val route = Screen.ParentChat.createRoute(
                        conversationId = "new", // Placeholder, will be created on first message
                        recipientId = teacherId,
                        recipientName = teacherName
                    )
                    
                    android.util.Log.d("ParentNavGraph", "Navigating to route: $route")
                    navController.navigate(route)
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ParentSelectGroupParticipants.route) {
            val selectViewModel: ParentSelectRecipientViewModel = hiltViewModel()
            val conversationViewModel: ConversationListViewModel = hiltViewModel()
            val conversationState by conversationViewModel.state.collectAsState()
            
            SelectGroupParticipantsScreen(
                selectRecipientViewModel = selectViewModel,
                conversationViewModel = conversationViewModel,
                onGroupCreated = { conversationId ->
                    // Sử dụng tên nhóm thực tế từ state
                    val groupTitle = conversationState.createdGroupTitle ?: "Nhóm"
                    navController.navigate(
                        Screen.ParentChat.createRoute(
                            conversationId = conversationId,
                            recipientId = "",
                            recipientName = groupTitle
                        )
                    ) {
                        popUpTo(Screen.ParentConversations.route)
                    }
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.ParentChat.routeWithArgs,
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
                android.util.Log.e("ParentNavGraph", "Error decoding recipientName: ${e.message}")
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
                        Screen.ParentGroupDetails.createRoute(convId, groupTitle)
                    )
                }
            )
        }
        
        composable(
            route = Screen.ParentGroupDetails.routeWithArgs,
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
                        Screen.ParentAddMembersToGroup.createRoute(convId)
                    )
                }
            )
        }
        
        composable(
            route = Screen.ParentAddMembersToGroup.routeWithArgs,
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
