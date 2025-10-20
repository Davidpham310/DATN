package com.example.datn.presentation.teacher.lessons.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.lesson.LessonManagerEvent
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.lessons.LessonManagerViewModel
import com.example.datn.presentation.teacher.lessons.components.AddEditLessonDialog
import com.example.datn.presentation.teacher.lessons.components.LessonList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonManagerScreen(
    classId: String,
    className: String,
    onNavigateBack: () -> Unit,
    onNavigateToContentManager: (lessonId: String, lessonTitle: String) -> Unit,
    viewModel: LessonManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Load lessons when screen opens
    LaunchedEffect(classId) {
        viewModel.onEvent(LessonManagerEvent.LoadLessonsForClass(classId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản lý bài học")
                        Text(
                            text = className,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(LessonManagerEvent.ShowAddLessonDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm bài học")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.lessons.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    LessonList(
                        lessons = state.lessons,
                        onEdit = { lesson ->
                            viewModel.onEvent(LessonManagerEvent.EditLesson(lesson))
                        },
                        onDelete = { lesson ->
                            viewModel.onEvent(LessonManagerEvent.DeleteLesson(lesson))
                        },
                        onClick = { lesson ->
                            viewModel.onEvent(LessonManagerEvent.SelectLesson(lesson))
                            onNavigateToContentManager(lesson.id, lesson.title)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Loading overlay
            if (state.isLoading && state.lessons.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Add/Edit Dialog
        if (state.showAddEditDialog) {
            AddEditLessonDialog(
                lesson = state.editingLesson,
                classId = classId,
                onDismiss = { viewModel.onEvent(LessonManagerEvent.DismissDialog) },
                onConfirmAdd = { title, description, contentLink->
                    viewModel.onEvent(
                        LessonManagerEvent.ConfirmAddLesson(
                            classId,title, description, contentLink
                        )
                    )
                },
                onConfirmEdit = { id, classId, title, description, contentLink, order ->
                    viewModel.onEvent(
                        LessonManagerEvent.ConfirmEditLesson(
                            id, classId, title, description, contentLink, order
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
            onConfirm = { lesson -> viewModel.confirmDeleteLesson(lesson) }
        )
    }
}