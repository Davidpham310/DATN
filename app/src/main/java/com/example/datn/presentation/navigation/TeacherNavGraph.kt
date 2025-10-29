package com.example.datn.presentation.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
//            Log.d("TeacherNavGraph", "lessonId: $lessonId")
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
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("lessonTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val lessonTitle = backStackEntry.arguments?.getString("lessonTitle") ?: ""
            LessonTestManagerScreen(
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

    }
}
