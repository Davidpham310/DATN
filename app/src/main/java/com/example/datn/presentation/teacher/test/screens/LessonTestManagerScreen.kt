package com.example.datn.presentation.teacher.test.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.test.TestEvent
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.test.components.AddEditTestDialog
import com.example.datn.presentation.teacher.test.components.TestList
import com.example.datn.presentation.teacher.test.viewmodel.TestManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonTestManagerScreen(
    lessonId: String,
    lessonTitle: String,
    classId: String,
    onNavigateToQuestions: (testId: String, testTitle: String) -> Unit,
    onNavigateToSubmissions: (testId: String, testTitle: String) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: TestManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(lessonId) {
        viewModel.onEvent(TestEvent.LoadTests(lessonId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Bài kiểm tra - $lessonTitle",
                        fontWeight = FontWeight.Bold
                    ) 
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(TestEvent.ShowAddTestDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm bài kiểm tra")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            } else {
                TestList(
                    tests = state.tests,
                    onTestClick = { test -> onNavigateToQuestions(test.id, test.title) },
                    onGradeClick = { test -> onNavigateToSubmissions(test.id, test.title) },
                    onEdit = { test -> viewModel.onEvent(TestEvent.EditTest(test)) },
                    onDelete = { test -> viewModel.onEvent(TestEvent.DeleteTest(test)) }
                )
            }
        }

        // Add/Edit Dialog
        if (state.showAddEditDialog) {
            AddEditTestDialog(
                test = state.editingTest,
                classId = classId,
                lessonId = lessonId,
                onDismiss = { viewModel.onEvent(TestEvent.DismissDialog) },
                onConfirmAdd = { cId, lId, title, desc, score, start, end ->
                    viewModel.onEvent(
                        TestEvent.ConfirmAddTest(cId, lId, title, desc, score, start, end)
                    )
                },
                onConfirmEdit = { id, cId, lId, title, desc, score, start, end ->
                    viewModel.onEvent(
                        TestEvent.ConfirmEditTest(id, cId, lId, title, desc, score, start, end)
                    )
                }
            )
        }

        // Delete Confirmation Dialog
        if (state.confirmDeleteState.isShowing) {
            ConfirmationDialog(
                state = state.confirmDeleteState,
                confirmText = "Xóa",
                onDismiss = { viewModel.dismissConfirmDeleteDialog() },
                onConfirm = { test -> viewModel.confirmDeleteTest(test) }
            )
        }
    }
}


