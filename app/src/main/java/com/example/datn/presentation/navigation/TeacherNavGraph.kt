package com.example.datn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.datn.presentation.teacher.home.TeacherHomeScreen

@Composable
fun TeacherNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TeacherHome.route,
        modifier = modifier // 👈 Thêm dòng này
    ) {
        composable(Screen.TeacherHome.route) { TeacherHomeScreen() }
//        composable(Screen.TeacherClassManager.route) { ClassManagerScreen(navController) }
//        composable(Screen.TeacherAssignmentManager.route) { AssignmentManagerScreen(navController) }
//        composable(Screen.TeacherSchedule.route) { TeacherScheduleScreen(navController) }
//        composable(Screen.TeacherNotification.route) { TeacherNotificationScreen(navController) }
    }
}
