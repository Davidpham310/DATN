package com.example.datn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.datn.presentation.parent.account.ParentAccountScreen
import com.example.datn.presentation.parent.home.ParentHomeScreen
import com.example.datn.presentation.parent.messaging.ParentChatScreen
import com.example.datn.presentation.parent.messaging.ParentConversationListScreen
import com.example.datn.presentation.parent.messaging.SelectTeacherScreen
import com.example.datn.presentation.parent.studentmanagement.ParentStudentManagementScreen
import com.example.datn.presentation.parent.studentprofile.StudentDetailScreen

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
        
        composable(Screen.ParentConversations.route) {
            ParentConversationListScreen(navController = navController)
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
            
            // Debug log
            android.util.Log.d("ParentNavGraph", "ChatScreen arguments - conversationId: '$conversationId', recipientId: '$recipientId', recipientName: '$recipientName'")
            
            if (recipientId.isBlank()) {
                android.util.Log.e("ParentNavGraph", "ERROR: recipientId is blank in ChatScreen!")
            }
            
            ParentChatScreen(
                conversationId = conversationId,
                recipientId = recipientId,
                recipientName = recipientName,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
