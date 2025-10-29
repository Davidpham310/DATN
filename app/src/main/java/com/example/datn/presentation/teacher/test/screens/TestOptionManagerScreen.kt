package com.example.datn.presentation.teacher.test.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.test.TestOptionEvent
import com.example.datn.presentation.teacher.test.TestOptionViewModel
import com.example.datn.presentation.teacher.test.components.AddEditTestOptionDialog
import com.example.datn.presentation.teacher.test.components.TestOptionList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestOptionManagerScreen(
    questionId: String,
    questionContent: String,
    onNavigateBack: () -> Unit,
    viewModel: TestOptionViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(questionId) {
        viewModel.setQuestionId(questionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Đáp án: $questionContent") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(TestOptionEvent.ShowAddOptionDialog) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quản lý đáp án",
                style = MaterialTheme.typography.titleMedium
            )

            TestOptionList(
                options = state.value.options,
                onEdit = { viewModel.onEvent(TestOptionEvent.EditOption(it)) },
                onDelete = { viewModel.onEvent(TestOptionEvent.DeleteOption(it)) },
                onClick = { /* no-op */ }
            )
        }

        if (state.value.showAddEditDialog) {
            AddEditTestOptionDialog(
                editing = state.value.editingOption,
                onDismiss = { viewModel.onEvent(TestOptionEvent.DismissDialog) },
                onConfirm = { content, isCorrect, mediaUrl ->
                    if (state.value.editingOption == null) {
                        viewModel.onEvent(
                            TestOptionEvent.ConfirmAddOption(
                                questionId = questionId,
                                content = content,
                                isCorrect = isCorrect,
                                mediaUrl = mediaUrl
                            )
                        )
                    } else {
                        viewModel.onEvent(
                            TestOptionEvent.ConfirmEditOption(
                                id = state.value.editingOption!!.id,
                                questionId = questionId,
                                content = content,
                                isCorrect = isCorrect,
                                mediaUrl = mediaUrl
                            )
                        )
                    }
                }
            )
        }
    }
}


