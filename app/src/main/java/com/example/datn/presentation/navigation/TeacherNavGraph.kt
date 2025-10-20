package com.example.datn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IAuthRepository
import com.example.datn.presentation.teacher.classes.screens.ClassManagerScreen
import com.example.datn.presentation.teacher.home.TeacherHomeScreen
import com.example.datn.presentation.teacher.lessons.screens.LessonContentManagerScreen
import com.example.datn.presentation.teacher.lessons.screens.LessonManagerScreen

@Composable
fun TeacherNavGraph(
    navController: NavHostController,
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
                }
            )
        }
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
                        Screen.TeacherLessonContentManager.createRoute(lessonId, lessonTitle)
                    )
                }
            )
        }
        composable(
            route = Screen.TeacherLessonContentManager.routeWithArgs,
            arguments = listOf(
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("lessonTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val lessonTitle = backStackEntry.arguments?.getString("lessonTitle") ?: ""
            LessonContentManagerScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onNavigateBack = { navController.popBackStack() },

            )
        }
    }
}
