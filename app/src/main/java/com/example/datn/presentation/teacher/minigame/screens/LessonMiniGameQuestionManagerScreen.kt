package com.example.datn.presentation.teacher.minigame.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.minigame.MiniGameQuestionEvent
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.minigame.components.*
import com.example.datn.presentation.teacher.minigame.viewmodel.LessonMiniGameQuestionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonMiniGameQuestionManagerScreen(
    gameId: String,
    gameTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToOptions: (questionId: String, questionContent: String) -> Unit,
    viewModel: LessonMiniGameQuestionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Initialize with gameId
    LaunchedEffect(gameId) {
        viewModel.onEvent(MiniGameQuestionEvent.LoadQuestionsForGame(gameId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý câu hỏi - $gameTitle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(MiniGameQuestionEvent.ShowAddQuestionDialog) }
            ) {
                Icon(Icons.Default.Add, "Thêm câu hỏi")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading && state.questions.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    state.questions.isEmpty() && !state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Chưa có câu hỏi nào",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Nhấn nút + để thêm câu hỏi đầu tiên",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    else -> {
                        MiniGameQuestionList(
                            questions = state.questions,
                            onEdit = { question ->
                                viewModel.onEvent(MiniGameQuestionEvent.EditQuestion(question))
                            },
                            onDelete = { question ->
                                viewModel.onEvent(MiniGameQuestionEvent.DeleteQuestion(question))
                            },
                            onClick = { q -> onNavigateToOptions(q.id, q.content) },
                            modifier = Modifier.fillMaxSize(),
                            questionOptions = state.questionOptions
                        )
                    }
                }

                // Loading overlay
                if (state.isLoading && state.questions.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (state.showAddEditDialog) {
            AddEditQuestionDialog(
                question = state.editingQuestion,
                gameId = gameId,
                onDismiss = { viewModel.onEvent(MiniGameQuestionEvent.DismissDialog) },
                onConfirmAdd = { gameId, content, questionType, score, timeLimit, order ->
                    viewModel.onEvent(
                        MiniGameQuestionEvent.ConfirmAddQuestion(
                            gameId, content, questionType, score, timeLimit, order
                        )
                    )
                },
                onConfirmEdit = { id, gameId, content, questionType, score, timeLimit, order ->
                    viewModel.onEvent(
                        MiniGameQuestionEvent.ConfirmEditQuestion(
                            id, gameId, content, questionType, score, timeLimit, order
                        )
                    )
                }
            )
        }

        // Delete Confirmation Dialog
        ConfirmationDialog(
            state = state.confirmDeleteState,
            confirmText = "Xóa",
            onDismiss = { viewModel.dismissConfirmDeleteDialog() },
            onConfirm = { question -> viewModel.confirmDeleteQuestion(question) }
        )
    }
}
