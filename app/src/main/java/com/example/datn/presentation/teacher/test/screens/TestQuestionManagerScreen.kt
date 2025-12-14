package com.example.datn.presentation.teacher.test.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.TestQuestion
import com.example.datn.presentation.common.test.TestQuestionEvent
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.test.components.AddEditTestQuestionDialog
import com.example.datn.presentation.teacher.test.viewmodel.TestQuestionManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestQuestionManagerScreen(
    testId: String,
    testTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToOptions: (questionId: String, content: String) -> Unit,
    viewModel: TestQuestionManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val excelPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(it)
                        if (inputStream != null) {
                            viewModel.importFromExcel(testId, inputStream)
                        }
                    } catch (_: Exception) {
                        // ViewModel will surface errors via notifications on import flow
                    }
                }
            }
        }

        LaunchedEffect(testId) {
            viewModel.setTest(testId, testTitle)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Quản lý câu hỏi",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                excelPickerLauncher.launch(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBox,
                                contentDescription = "Import Excel",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.onEvent(TestQuestionEvent.ShowAddQuestionDialog)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm câu hỏi")
                }
            }
        )  { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    state.isLoading && state.questions.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    state.questions.isEmpty() -> {
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
                                text = "Chưa có câu hỏi nào",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nhấn nút + để thêm câu hỏi mới",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
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
                                SummaryHeader(
                                    totalQuestions = state.questions.size,
                                    totalScore = state.questions.sumOf { it.score }
                                )
                            }

                            // Danh sách câu hỏi
                            items(state.questions, key = { it.id }) { question ->
                                val index = state.questions.indexOf(question) + 1
                                TestQuestionItem(
                                    questionNumber = index,
                                    question = question,
                                    onEdit = { viewModel.onEvent(TestQuestionEvent.EditQuestion(question)) },
                                    onDelete = { viewModel.onEvent(TestQuestionEvent.DeleteQuestion(question)) },
                                    onClick = { onNavigateToOptions(question.id, question.content) }
                                )
                            }
                        }
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

            // Add/Edit Dialog
            if (state.showAddEditDialog) {
                AddEditTestQuestionDialog(
                    testQuestion = state.editingQuestion,
                    onDismiss = { viewModel.onEvent(TestQuestionEvent.DismissDialog) },
                    onConfirm = { content, score, questionType, mediaUrl ->
                        if (state.editingQuestion == null) {
                            viewModel.onEvent(
                                TestQuestionEvent.ConfirmAddQuestion(
                                    testId = testId,
                                    content = content,
                                    score = score,
                                    questionType = questionType,
                                    mediaUrl = mediaUrl
                                )
                            )
                        } else {
                            viewModel.onEvent(
                                TestQuestionEvent.ConfirmEditQuestion(
                                    id = state.editingQuestion!!.id,
                                    testId = testId,
                                    content = content,
                                    score = score,
                                    questionType = questionType,
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
                onConfirm = { question -> viewModel.confirmDeleteQuestion(question) }
            )
        }
    }

    @Composable
    private fun SummaryHeader(
        totalQuestions: Int,
        totalScore: Double
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalQuestions.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Câu hỏi",
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
                        text = totalScore.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Tổng điểm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    @Composable
    private fun TestQuestionItem(
        questionNumber: Int,
        question: TestQuestion,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header: Number + Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Question number badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Câu $questionNumber",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Sửa",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Question content (clickable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = question.content,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Metadata row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Score
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "${question.score} điểm",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Question type
                            Text(
                                text = question.questionType.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Media indicator
                            if (!question.mediaUrl.isNullOrBlank()) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Có hình ảnh",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Navigate arrow
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Xem đáp án",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }


