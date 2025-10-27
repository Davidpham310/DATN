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
import com.example.datn.presentation.common.minigame.MiniGameManagerEvent
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.minigame.LessonMiniGameManagerViewModel
import com.example.datn.presentation.teacher.minigame.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonMiniGameManagerScreen(
    lessonId: String,
    lessonTitle: String,
    onNavigateToQuestions: (gameId: String, gameTitle: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LessonMiniGameManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Initialize with lessonId
    LaunchedEffect(lessonId) {
        viewModel.onEvent(MiniGameManagerEvent.LoadGamesForLesson(lessonId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản lý Mini Game")
                        Text(
                            text = lessonTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(MiniGameManagerEvent.ShowAddGameDialog) }
            ) {
                Icon(Icons.Default.Add, "Thêm mini game")
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
                    state.isLoading && state.miniGames.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    state.miniGames.isEmpty() && !state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Chưa có mini game nào",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Nhấn nút + để thêm mini game đầu tiên",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    else -> {
                        MiniGameList(
                            games = state.miniGames,
                            onEdit = { game ->
                                viewModel.onEvent(MiniGameManagerEvent.EditGame(game))
                            },
                            onDelete = { game ->
                                viewModel.onEvent(MiniGameManagerEvent.DeleteGame(game))
                            },
                            onClick = { game ->
                                viewModel.onEvent(MiniGameManagerEvent.SelectGame(game))
                                onNavigateToQuestions(game.id, game.title)
                            }
                        )
                    }
                }

                // Loading overlay
                if (state.isLoading && state.miniGames.isNotEmpty()) {
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
            AddEditMiniGameDialog(
                game = state.editingGame,
                onDismiss = { viewModel.onEvent(MiniGameManagerEvent.DismissDialog) },
                onConfirmAdd = { title, description, gameType, level, contentUrl ->
                    viewModel.onEvent(
                        MiniGameManagerEvent.ConfirmAddGame(
                            lessonId = lessonId,
                             title = title,
                             description = description,
                             gameType = gameType,
                             level = level,
                             contentUrl = contentUrl
                        )
                    )
                },
                onConfirmEdit = { id, title, description, gameType, level, contentUrl ->
                    viewModel.onEvent(
                        MiniGameManagerEvent.ConfirmEditGame(
                            id = id,
                            lessonId = lessonId,
                            title = title,
                            description = description,
                            gameType = gameType,
                            level = level,
                            contentUrl = contentUrl
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
            onConfirm = { game -> viewModel.confirmDeleteGame(game) }
        )
    }
}
