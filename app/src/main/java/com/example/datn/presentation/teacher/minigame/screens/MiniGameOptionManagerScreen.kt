package com.example.datn.presentation.teacher.minigame.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.minigame.MiniGameOptionEvent
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.minigame.MiniGameOptionViewModel
import com.example.datn.presentation.teacher.minigame.components.AddEditMiniGameOptionDialog
import com.example.datn.presentation.teacher.minigame.components.MiniGameOptionList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGameOptionManagerScreen(
    questionId: String,
    questionContent: String,
    onNavigateBack: () -> Unit,
    viewModel: MiniGameOptionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(questionId) {
        viewModel.onEvent(MiniGameOptionEvent.LoadOptionsForQuestion(questionId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đáp án: $questionContent") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onEvent(MiniGameOptionEvent.ShowAddOptionDialog) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            MiniGameOptionList(
                options = state.options,
                onEdit = { viewModel.onEvent(MiniGameOptionEvent.EditOption(it)) },
                onDelete = { viewModel.onEvent(MiniGameOptionEvent.DeleteOption(it)) },
                onClick = { viewModel.onEvent(MiniGameOptionEvent.SelectOption(it)) }
            )
        }

        if (state.showAddEditDialog) {
            AddEditMiniGameOptionDialog(
                editing = state.editingOption,
                questionType = state.currentQuestionType,
                gameType = state.currentGameType,
                onDismiss = { viewModel.onEvent(MiniGameOptionEvent.DismissDialog) },
                onConfirm = { content, isCorrect, mediaUrl, hint, pairContent ->
                    val qid = state.currentQuestionId
                    val editing = state.editingOption
                    if (editing == null) {
                        viewModel.onEvent(
                            MiniGameOptionEvent.ConfirmAddOption(
                                questionId = qid,
                                content = content,
                                isCorrect = isCorrect,
                                mediaUrl = mediaUrl,
                                hint = hint,
                                pairContent = pairContent
                            )
                        )
                    } else {
                        viewModel.onEvent(
                            MiniGameOptionEvent.ConfirmEditOption(
                                id = editing.id,
                                questionId = qid,
                                content = content,
                                isCorrect = isCorrect,
                                mediaUrl = mediaUrl,
                                hint = hint,
                                pairContent = pairContent
                            )
                        )
                    }
                }
            )
        }

        if (state.confirmDeleteState.isShowing) {
            ConfirmationDialog(state = state.confirmDeleteState, onDismiss = { viewModel.dismissConfirmDeleteDialog() }, onConfirm = { item ->
                viewModel.confirmDeleteOption(item)
            })
        }
    }
}


