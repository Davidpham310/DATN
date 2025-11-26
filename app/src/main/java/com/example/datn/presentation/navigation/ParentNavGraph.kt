package com.example.datn.presentation.navigation

import androidx.navigation.NavArgument
import androidx.navigation.NavType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.datn.presentation.parent.account.ParentAccountScreen
import com.example.datn.presentation.parent.home.ParentHomeScreen
import com.example.datn.presentation.teacher.studentdetail.StudentDetailScreen

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
            ParentHomeScreen(
                onNavigateToStudentDetail = { studentId, studentName ->
                    navController.navigate(
                        Screen.ParentStudentDetail.createRoute(studentId, studentName)
                    )
                }
            )
        }

        composable(Screen.ParentAccount.route) {
            ParentAccountScreen(onNavigateToLogin = onNavigateToLogin)
        }

        composable(
            route = Screen.ParentStudentDetail.routeWithArgs,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType },
                navArgument("studentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val studentName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("studentName") ?: "", "UTF-8"
            )
            StudentDetailScreen(
                studentId = studentId,
                classId = "", // Parent xem toàn bộ lớp, không lọc theo lớp cụ thể
                studentName = studentName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
