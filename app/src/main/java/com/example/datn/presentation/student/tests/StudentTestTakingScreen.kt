package com.example.datn.presentation.student.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.QuestionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTestTakingScreen(
    testId: String,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (String, String) -> Unit,
    viewModel: StudentTestTakingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(testId) {
        viewModel.onEvent(StudentTestTakingEvent.LoadTest(testId))
    }

    // Navigate to result after successful submission
    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted && state.test != null) {
            // Navigate to result screen with testId
            onNavigateToResult(state.test!!.id, "") // resultId will be fetched in result screen
        }
    }

    // Submit Dialog
    if (state.showSubmitDialog) {
        SubmitConfirmationDialog(
            answeredCount = state.answers.size,
            totalCount = state.questions.size,
            onConfirm = { viewModel.onEvent(StudentTestTakingEvent.ConfirmSubmit) },
            onDismiss = { viewModel.onEvent(StudentTestTakingEvent.DismissSubmitDialog) }
        )
    }

    // Question List Dialog
    if (state.showQuestionList) {
        QuestionListDialog(
            questions = state.questions,
            currentIndex = state.currentQuestionIndex,
            answers = state.answers,
            onQuestionSelected = { index ->
                viewModel.onEvent(StudentTestTakingEvent.GoToQuestion(index))
            },
            onDismiss = { viewModel.onEvent(StudentTestTakingEvent.ToggleQuestionList) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.test?.title ?: "Bài Kiểm Tra",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = state.getFormattedTimeRemaining(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.timeRemaining < 300) // < 5 minutes
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(StudentTestTakingEvent.ToggleQuestionList) }) {
                        Icon(Icons.Default.List, contentDescription = "Danh sách câu hỏi")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    OutlinedButton(
                        onClick = { viewModel.onEvent(StudentTestTakingEvent.PreviousQuestion) },
                        enabled = state.canGoPrevious
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trước")
                    }

                    // Question Counter
                    Text(
                        text = "${state.currentQuestionIndex + 1}/${state.questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Next Button
                    if (state.canGoNext) {
                        Button(
                            onClick = { viewModel.onEvent(StudentTestTakingEvent.NextQuestion) }
                        ) {
                            Text("Sau")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.onEvent(StudentTestTakingEvent.ShowSubmitDialog) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nộp bài")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error ?: "Đã xảy ra lỗi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                state.currentQuestion != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Progress Bar
                        LinearProgressIndicator(
                            progress = state.progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Đã trả lời ${state.answers.size}/${state.questions.size} câu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Question Content
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                QuestionContent(
                                    question = state.currentQuestion!!,
                                    currentAnswer = state.answers[state.currentQuestion!!.question.id],
                                    onAnswerChanged = { answer ->
                                        val question = state.currentQuestion!!.question
                                        when (question.questionType) {
                                            QuestionType.SINGLE_CHOICE -> {
                                                (answer as? Answer.SingleChoice)?.let {
                                                    viewModel.onEvent(
                                                        StudentTestTakingEvent.AnswerSingleChoice(
                                                            question.id,
                                                            it.optionId
                                                        )
                                                    )
                                                }
                                            }
                                            QuestionType.MULTIPLE_CHOICE -> {
                                                (answer as? Answer.MultipleChoice)?.let {
                                                    viewModel.onEvent(
                                                        StudentTestTakingEvent.AnswerMultipleChoice(
                                                            question.id,
                                                            it.optionIds
                                                        )
                                                    )
                                                }
                                            }
                                            QuestionType.FILL_BLANK -> {
                                                (answer as? Answer.FillBlank)?.let {
                                                    viewModel.onEvent(
                                                        StudentTestTakingEvent.AnswerFillBlank(
                                                            question.id,
                                                            it.text
                                                        )
                                                    )
                                                }
                                            }
                                            QuestionType.ESSAY -> {
                                                (answer as? Answer.Essay)?.let {
                                                    viewModel.onEvent(
                                                        StudentTestTakingEvent.AnswerEssay(
                                                            question.id,
                                                            it.text
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(
    question: QuestionWithOptions,
    currentAnswer: Answer?,
    onAnswerChanged: (Answer) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Question Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Câu ${question.question.order}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${question.question.score} điểm",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question Text
            Text(
                text = question.question.content,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Answer Options based on type
            when (question.question.questionType) {
                QuestionType.SINGLE_CHOICE -> {
                    SingleChoiceOptions(
                        options = question.options,
                        selectedOptionId = (currentAnswer as? Answer.SingleChoice)?.optionId,
                        onOptionSelected = { optionId ->
                            onAnswerChanged(Answer.SingleChoice(optionId))
                        }
                    )
                }
                QuestionType.MULTIPLE_CHOICE -> {
                    MultipleChoiceOptions(
                        options = question.options,
                        selectedOptionIds = (currentAnswer as? Answer.MultipleChoice)?.optionIds ?: emptySet(),
                        onOptionsChanged = { optionIds ->
                            onAnswerChanged(Answer.MultipleChoice(optionIds))
                        }
                    )
                }
                QuestionType.FILL_BLANK -> {
                    FillBlankInput(
                        text = (currentAnswer as? Answer.FillBlank)?.text ?: "",
                        onTextChanged = { text ->
                            onAnswerChanged(Answer.FillBlank(text))
                        }
                    )
                }
                QuestionType.ESSAY -> {
                    EssayInput(
                        text = (currentAnswer as? Answer.Essay)?.text ?: "",
                        onTextChanged = { text ->
                            onAnswerChanged(Answer.Essay(text))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleChoiceOptions(
    options: List<com.example.datn.domain.models.TestOption>,
    selectedOptionId: String?,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (option.id == selectedOptionId)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option.id == selectedOptionId,
                    onClick = { onOptionSelected(option.id) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MultipleChoiceOptions(
    options: List<com.example.datn.domain.models.TestOption>,
    selectedOptionIds: Set<String>,
    onOptionsChanged: (Set<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (option.id in selectedOptionIds)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = option.id in selectedOptionIds,
                    onCheckedChange = { checked ->
                        val newSelection = if (checked) {
                            selectedOptionIds + option.id
                        } else {
                            selectedOptionIds - option.id
                        }
                        onOptionsChanged(newSelection)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun FillBlankInput(
    text: String,
    onTextChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Nhập câu trả lời") },
        placeholder = { Text("Nhập câu trả lời của bạn...") }
    )
}

@Composable
private fun EssayInput(
    text: String,
    onTextChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        label = { Text("Nhập bài luận") },
        placeholder = { Text("Viết bài luận của bạn...") },
        maxLines = 10
    )
}

@Composable
private fun SubmitConfirmationDialog(
    answeredCount: Int,
    totalCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác nhận nộp bài") },
        text = {
            Column {
                Text("Bạn đã trả lời $answeredCount/$totalCount câu.")
                if (answeredCount < totalCount) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Còn ${totalCount - answeredCount} câu chưa trả lời. Bạn có chắc muốn nộp bài?",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Nộp bài")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun QuestionListDialog(
    questions: List<QuestionWithOptions>,
    currentIndex: Int,
    answers: Map<String, Answer>,
    onQuestionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Danh sách câu hỏi") },
        text = {
            LazyColumn {
                items(questions.size) { index ->
                    val question = questions[index]
                    val isAnswered = answers.containsKey(question.question.id)
                    val isCurrent = index == currentIndex

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            onQuestionSelected(index)
                            onDismiss()
                        },
                        color = if (isCurrent)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Câu ${index + 1}")
                            if (isAnswered) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
