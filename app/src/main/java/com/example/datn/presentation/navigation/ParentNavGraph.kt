package com.example.datn.presentation.navigation

import androidx.navigation.NavType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.datn.presentation.parent.account.ParentAccountScreen
import com.example.datn.presentation.parent.home.ParentHomeScreen
import com.example.datn.presentation.parent.classlist.ParentClassListScreen
import com.example.datn.presentation.parent.classmanager.ParentJoinClassScreen
import com.example.datn.presentation.parent.studentprofile.StudentDetailScreen
import com.example.datn.presentation.parent.messaging.ParentSelectRecipientScreen
import com.example.datn.presentation.common.messaging.screens.ConversationListScreen
import com.example.datn.presentation.common.messaging.screens.ChatScreen
import com.example.datn.presentation.common.messaging.screens.SelectGroupParticipantsScreen
import com.example.datn.presentation.common.messaging.screens.GroupDetailsScreen
import com.example.datn.presentation.common.messaging.screens.AddMembersToGroupScreen
import com.example.datn.presentation.common.messaging.ConversationListViewModel
import com.example.datn.presentation.common.messaging.ChatViewModel
import com.example.datn.presentation.teacher.messaging.SelectRecipientViewModel

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
        // Home - thông tin con em
        composable(Screen.ParentHome.route) {
            ParentHomeScreen(
                onNavigateToStudentDetail = { studentId, studentName ->
                    navController.navigate(
                        Screen.ParentStudentDetail.createRoute(studentId, studentName)
                    )
                },
                onNavigateToManageChildren = {
                    navController.navigate(Screen.ParentManageChildren.route)
                }
            )
        }

        // Danh sách lớp học của con
        composable(Screen.ParentMyClasses.route) {
            ParentClassListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClassDetail = { _ ->
                    // Tạm thời chưa có màn chi tiết lớp cho phụ huynh
                },
                onNavigateToJoinClass = {
                    navController.navigate(Screen.ParentJoinClass.route)
                }
            )
        }

        // Tìm kiếm / xin tham gia lớp cho con
        composable(Screen.ParentJoinClass.route) {
            ParentJoinClassScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Danh sách cuộc hội thoại
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
                    navController.navigate(Screen.ParentSelectRecipient.route)
                },
                onGroupChatClick = {
                    navController.navigate(Screen.ParentSelectGroupParticipants.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Chọn người nhận tin nhắn mới (con, giáo viên, phụ huynh khác)
        composable(Screen.ParentSelectRecipient.route) {
            ParentSelectRecipientScreen(
                onRecipientSelected = { recipientId, recipientName ->
                    val route = Screen.ParentChat.createRoute(
                        conversationId = "new",
                        recipientId = recipientId,
                        recipientName = recipientName
                    )
                    navController.navigate(route)
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        // Chọn thành viên để tạo nhóm chat (dùng SelectRecipientViewModel chung: Parents/Students)
        composable(Screen.ParentSelectGroupParticipants.route) {
            val selectViewModel: SelectRecipientViewModel = hiltViewModel()
            val conversationViewModel: ConversationListViewModel = hiltViewModel()
            val conversationState by conversationViewModel.state.collectAsState()

            SelectGroupParticipantsScreen(
                selectRecipientViewModel = selectViewModel,
                conversationViewModel = conversationViewModel,
                onGroupCreated = { conversationId ->
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

        // Màn hình chat (1-1 hoặc nhóm)
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

        // Thông tin nhóm chat
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

        // Thêm thành viên vào nhóm chat
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

        // Account
        composable(Screen.ParentAccount.route) {
            ParentAccountScreen(onNavigateToLogin = onNavigateToLogin)
        }

        // Quản lý con em (stub màn hình, sẽ triển khai đầy đủ logic sau)
        composable(Screen.ParentManageChildren.route) {
            com.example.datn.presentation.parent.managechildren.ParentManageChildrenScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateStudentAccount = {
                    navController.navigate(Screen.ParentCreateStudentAccount.route)
                }
            )
        }

        // Tạo tài khoản học sinh mới (stub màn hình)
        composable(Screen.ParentCreateStudentAccount.route) {
            com.example.datn.presentation.parent.managechildren.ParentCreateStudentAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Chi tiết học sinh (sử dụng màn Parent-specific)
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
                studentName = studentName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
