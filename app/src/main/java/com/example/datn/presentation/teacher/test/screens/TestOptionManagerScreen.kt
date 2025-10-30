package com.example.datn.presentation.teacher.test.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.test.TestOptionEvent
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.test.TestOptionViewModel
import com.example.datn.presentation.teacher.test.components.AddEditTestOptionDialog
import com.example.datn.presentation.teacher.test.components.TestOptionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestOptionManagerScreen(
    questionId: String,
    questionContent: String,
    onNavigateBack: () -> Unit,
    viewModel: TestOptionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(questionId) {
        viewModel.setQuestionId(questionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản lý đáp án")
                        Text(
                            text = questionContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(TestOptionEvent.ShowAddOptionDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm đáp án")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.options.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.options.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có đáp án nào",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nhấn nút + để thêm đáp án mới",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        // Hiển thị thông tin loại câu hỏi nếu có
                        state.currentQuestionType?.let { type ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Text(
                                    text = "Loại: ${type.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header thống kê
                        item {
                            OptionSummaryHeader(
                                totalOptions = state.options.size,
                                correctCount = state.options.count { it.isCorrect },
                                questionType = state.currentQuestionType
                            )
                        }

                        // Danh sách đáp án
                        items(state.options, key = { it.id }) { option ->
                            val index = state.options.indexOf(option) + 1
                            TestOptionItem(
                                optionNumber = index,
                                option = option,
                                onEdit = { viewModel.onEvent(TestOptionEvent.EditOption(option)) },
                                onDelete = { viewModel.onEvent(TestOptionEvent.DeleteOption(option)) },
                                onClick = {}
                            )
                        }
                    }
                }
            }

            // Loading overlay
            if (state.isLoading && state.options.isNotEmpty()) {
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
            AddEditTestOptionDialog(
                editing = state.editingOption,
                onDismiss = { viewModel.onEvent(TestOptionEvent.DismissDialog) },
                onConfirm = { content, isCorrect, mediaUrl ->
                    if (state.editingOption == null) {
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
                                id = state.editingOption!!.id,
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

        // Delete Confirmation Dialog
        ConfirmationDialog(
            state = state.confirmDeleteState,
            confirmText = "Xóa",
            onDismiss = { viewModel.dismissConfirmDeleteDialog() },
            onConfirm = { option -> viewModel.confirmDeleteOption(option) }
        )
    }
}

@Composable
private fun OptionSummaryHeader(
    totalOptions: Int,
    correctCount: Int,
    questionType: com.example.datn.domain.models.QuestionType?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalOptions.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Đáp án",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                VerticalDivider(
                    modifier = Modifier.height(48.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = correctCount.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Đáp án đúng",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            questionType?.let { type ->
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                )
                Text(
                    text = "Loại câu hỏi: ${type.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}


