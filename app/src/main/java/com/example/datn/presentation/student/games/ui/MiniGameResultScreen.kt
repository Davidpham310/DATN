package com.example.datn.presentation.student.games.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.datn.presentation.student.tests.state.Answer
import com.example.datn.presentation.student.games.state.QuestionWithAnswer
import com.example.datn.presentation.student.games.viewmodel.MiniGameResultViewModel
import com.example.datn.presentation.student.games.event.MiniGameResultEvent
import com.example.datn.core.utils.extensions.formatScore
import com.example.datn.presentation.student.games.state.MiniGameResultState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGameResultScreen(
    miniGameId: String,
    resultId: String,
    onNavigateBack: () -> Unit,
    onPlayAgain: () -> Unit = {},
    viewModel: MiniGameResultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(miniGameId, resultId) {
        viewModel.onEvent(MiniGameResultEvent.LoadResult(miniGameId, resultId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.miniGame?.title ?: "Kết Quả Mini Game") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(MiniGameResultEvent.ToggleDetailedAnswers) }
                    ) {
                        Icon(
                            if (state.showDetailedAnswers) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Hiện/Ẩn chi tiết"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Play Again Button
            if (state.miniGame != null && state.result != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp,
                    shadowElevation = 3.dp
                ) {
                    Button(
                        onClick = {
                            viewModel.onEvent(MiniGameResultEvent.PlayAgain)
                            onPlayAgain()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎮 CHƠI LẠI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                state.miniGame != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Score Summary Card
                        item {
                            MiniGameScoreSummaryCard(state = state)
                        }

                        // Best Score Card (if has multiple attempts)
                        if (state.attemptCount > 1) {
                            item {
                                BestScoreCard(state = state)
                            }
                        }

                        // Attempts History Card (if has multiple attempts)
                        if (state.attemptCount > 1) {
                            item {
                                AttemptsHistoryCard(state = state)
                            }
                        }

                        // Detailed answers section
                        if (state.showDetailedAnswers && state.questions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Chi Tiết Câu Trả Lời",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(state.questions) { questionWithAnswer ->
                                MiniGameQuestionResultCard(
                                    questionWithAnswer = questionWithAnswer
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
private fun MiniGameScoreSummaryCard(state: MiniGameResultState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Grade badge
            Text(
                text = state.gradeText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Score stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.result?.score?.formatScore() ?: "0"}/${state.result?.maxScore?.formatScore() ?: "0"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Điểm số",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Correct count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.correctCount}/${state.questions.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Câu đúng",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.durationText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Thời gian",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BestScoreCard(state: MiniGameResultState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700), // Gold
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "🏆 Best Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Attempt #${state.result?.attemptNumber ?: 1} of ${state.attemptCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = state.bestScore?.formatScore() ?: "0",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
        }
    }
}

@Composable
private fun AttemptsHistoryCard(state: MiniGameResultState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📊 Lịch Sử Attempts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            state.allResults.take(5).forEachIndexed { index, result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attempt #${result.attemptNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.score.formatScore(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (result.score == state.bestScore) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Best",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (result.id == state.result?.id) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(hiện tại)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (index < state.allResults.size - 1 && index < 4) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniGameQuestionResultCard(
    questionWithAnswer: QuestionWithAnswer
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (questionWithAnswer.isCorrect) 
                Color(0xFFE8F5E9) 
            else 
                Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Question header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Câu ${questionWithAnswer.question.order}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    if (questionWithAnswer.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (questionWithAnswer.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question content
            Text(
                text = questionWithAnswer.question.content,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options for multiple choice
            if (questionWithAnswer.question.questionType == QuestionType.SINGLE_CHOICE ||
                questionWithAnswer.question.questionType == QuestionType.MULTIPLE_CHOICE) {
                
                questionWithAnswer.options.forEach { option ->
                    val isStudentSelected = questionWithAnswer.studentSelectedIds.contains(option.id)
                    val isCorrectOption = option.isCorrect

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = when {
                                    isCorrectOption -> Color(0xFFC8E6C9)
                                    isStudentSelected && !isCorrectOption -> Color(0xFFFFCDD2)
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isStudentSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isCorrectOption) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (isCorrectOption && !isStudentSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = option.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Score
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Điểm: ${questionWithAnswer.earnedScore.formatScore()}/${questionWithAnswer.question.score.formatScore()}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (questionWithAnswer.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
