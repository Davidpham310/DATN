package com.example.datn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.example.datn.presentation.student.messaging.StudentChatScreen
import com.example.datn.presentation.student.messaging.StudentConversationListScreen

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
            StudentConversationListScreen(navController = navController)
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
            
            // Debug log
            android.util.Log.d("StudentNavGraph", "ChatScreen arguments - conversationId: '$conversationId', recipientId: '$recipientId', recipientName: '$recipientName'")
            
            if (recipientId.isBlank()) {
                android.util.Log.e("StudentNavGraph", "ERROR: recipientId is blank in ChatScreen!")
            }
            
            StudentChatScreen(
                conversationId = conversationId,
                recipientId = recipientId,
                recipientName = recipientName,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
