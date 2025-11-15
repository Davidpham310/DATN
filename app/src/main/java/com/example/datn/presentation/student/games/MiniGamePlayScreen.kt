package com.example.datn.presentation.student.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.datn.domain.models.GameType
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGamePlayScreen(
    miniGameId: String,
    onBack: () -> Unit,
    onGameComplete: (String) -> Unit,
    viewModel: MiniGamePlayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(miniGameId) {
        viewModel.onEvent(MiniGamePlayEvent.LoadMiniGame(miniGameId))
    }

    // Show result after successful submission - no navigation needed
    // Results are displayed directly in the game screen

    // Submit Dialog
    if (state.showSubmitDialog) {
        SubmitConfirmationDialog(
            answeredCount = calculateAnsweredQuestions(state),
            totalCount = state.questions.size,
            onConfirm = { viewModel.onEvent(MiniGamePlayEvent.ConfirmSubmit) },
            onDismiss = { viewModel.onEvent(MiniGamePlayEvent.DismissSubmitDialog) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = state.miniGame?.title ?: "Mini Game",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Timer
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.timeRemaining <= 60) 
                                MaterialTheme.colorScheme.errorContainer 
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = formatTime(state.timeRemaining),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = if (state.timeRemaining <= 60) 
                                MaterialTheme.colorScheme.onErrorContainer 
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading mini game...")
                    }
                }
            }
            
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
            
            state.questions.isNotEmpty() -> {
                if (state.isSubmitted) {
                    // Show results screen
                    GameResultsScreen(
                        state = state,
                        onPlayAgain = {
                            // Reset game state for play again
                            viewModel.onEvent(MiniGamePlayEvent.ResetGame)
                        },
                        onBackToLesson = onBack
                    )
                } else {
                    // Show game play screen
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Game info header
                        item {
                            GameInfoCard(
                                description = state.miniGame?.description ?: "",
                                totalQuestions = state.questions.size,
                                answeredQuestions = calculateAnsweredQuestions(state),
                                score = state.score
                            )
                        }
                        
                        // Questions
                        items(state.questions) { question ->
                            val questionOptions = state.questionOptions[question.id] ?: emptyList()
                            val selectedAnswer = if (question.questionType == QuestionType.MULTIPLE_CHOICE) {
                                state.multipleChoiceAnswers[question.id]?.joinToString(",") ?: ""
                            } else {
                                state.answers[question.id]
                            }
                            QuestionCard(
                                question = question,
                                options = questionOptions,
                                selectedAnswer = selectedAnswer,
                                onAnswerSelected = { answer ->
                                    viewModel.onEvent(
                                        MiniGamePlayEvent.AnswerQuestion(question.id, answer)
                                    )
                                },
                                onMultipleChoiceToggled = { optionId ->
                                    viewModel.onEvent(
                                        MiniGamePlayEvent.ToggleMultipleChoice(question.id, optionId)
                                    )
                                },
                                isSubmitted = state.isSubmitted
                            )
                        }
                        
                        // Submit button
                        item {
                            Button(
                                onClick = { 
                                    viewModel.onEvent(MiniGamePlayEvent.ShowSubmitDialog) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                enabled = !state.isSubmitted && calculateAnsweredQuestions(state) > 0
                            ) {
                                Text(
                                    text = if (state.isSubmitted) "Submitted" else "Submit Answers",
                                    fontWeight = FontWeight.Bold
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
private fun GameInfoCard(
    description: String,
    totalQuestions: Int,
    answeredQuestions: Int,
    score: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress: $answeredQuestions/$totalQuestions",
                    fontWeight = FontWeight.Medium
                )
                if (score > 0) {
                    Text(
                        text = "Score: $score%",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            LinearProgressIndicator(
                progress = if (totalQuestions > 0) answeredQuestions.toFloat() / totalQuestions else 0f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: com.example.datn.domain.models.MiniGameQuestion,
    options: List<com.example.datn.domain.models.MiniGameOption>,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onMultipleChoiceToggled: ((String) -> Unit)? = null,
    isSubmitted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Question ${question.order}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = question.content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            when (question.questionType) {
                QuestionType.SINGLE_CHOICE -> {
                    SingleChoiceOptions(
                        options = options,
                        selectedAnswer = selectedAnswer,
                        onAnswerSelected = onAnswerSelected,
                        isSubmitted = isSubmitted
                    )
                }
                
                QuestionType.MULTIPLE_CHOICE -> {
                    MultipleChoiceOptions(
                        options = options,
                        selectedAnswers = selectedAnswer?.split(",")?.toSet() ?: emptySet(),
                        onAnswerToggled = { optionId ->
                            if (!isSubmitted) {
                                onMultipleChoiceToggled?.invoke(optionId)
                            }
                        },
                        isSubmitted = isSubmitted
                    )
                }
                
                QuestionType.FILL_BLANK -> {
                    FillBlankQuestion(
                        selectedAnswer = selectedAnswer,
                        onAnswerChanged = onAnswerSelected,
                        isSubmitted = isSubmitted,
                        correctAnswer = if (isSubmitted) options.find { it.isCorrect }?.content else null
                    )
                }
                
                QuestionType.ESSAY -> {
                    val expectedKeywords = options.find { it.isCorrect }?.content
                    EssayQuestion(
                        selectedAnswer = selectedAnswer,
                        onAnswerChanged = onAnswerSelected,
                        isSubmitted = isSubmitted,
                        expectedKeywords = expectedKeywords
                    )
                }
            }
            
            if (isSubmitted) {
                val correctAnswer = options.find { it.isCorrect }?.content
                if (correctAnswer != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Correct answer: $correctAnswer",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
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
        title = { Text("Submit Mini Game?") },
        text = {
            Text("You have answered $answeredCount out of $totalCount questions. Are you sure you want to submit?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GameResultsScreen(
    state: MiniGamePlayState,
    onPlayAgain: () -> Unit,
    onBackToLesson: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result Header
        item {
            ResultHeaderCard(
                score = state.score,
                totalQuestions = state.questions.size,
                correctAnswers = calculateCorrectAnswers(state),
                gameTitle = state.miniGame?.title ?: "Mini Game"
            )
        }
        
        // Questions Review
        item {
            Text(
                text = "Review Your Answers",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        items(state.questions) { question ->
            val questionOptions = state.questionOptions[question.id] ?: emptyList()
            val selectedAnswer = if (question.questionType == com.example.datn.domain.models.QuestionType.MULTIPLE_CHOICE) {
                state.multipleChoiceAnswers[question.id]?.joinToString(",") ?: ""
            } else {
                state.answers[question.id]
            }
            QuestionCard(
                question = question,
                options = questionOptions,
                selectedAnswer = selectedAnswer,
                onAnswerSelected = { }, // Read-only in results
                onMultipleChoiceToggled = null, // Read-only in results
                isSubmitted = true // Always show as submitted in results
            )
        }
        
        // Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Again")
                }
                
                OutlinedButton(
                    onClick = onBackToLesson,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Lesson")
                }
            }
        }
    }
}

@Composable
private fun ResultHeaderCard(
    score: Int,
    totalQuestions: Int,
    correctAnswers: Int,
    gameTitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                score >= 80 -> MaterialTheme.colorScheme.primaryContainer
                score >= 60 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when {
                    score >= 80 -> Icons.Default.Star
                    score >= 60 -> Icons.Default.ThumbUp
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = when {
                    score >= 80 -> MaterialTheme.colorScheme.primary
                    score >= 60 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Text(
                text = "Game Completed!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = gameTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            score >= 80 -> MaterialTheme.colorScheme.primary
                            score >= 60 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$correctAnswers/$totalQuestions",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Correct",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Text(
                text = when {
                    score >= 90 -> "🎉 Excellent work!"
                    score >= 80 -> "👏 Great job!"
                    score >= 70 -> "👍 Good effort!"
                    score >= 60 -> "📚 Keep practicing!"
                    else -> "💪 Don't give up!"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun calculateAnsweredQuestions(state: MiniGamePlayState): Int {
    var answeredCount = 0
    state.questions.forEach { question ->
        val hasAnswer = when (question.questionType) {
            com.example.datn.domain.models.QuestionType.MULTIPLE_CHOICE -> {
                val selectedAnswers = state.multipleChoiceAnswers[question.id]
                !selectedAnswers.isNullOrEmpty()
            }
            else -> {
                val answer = state.answers[question.id]
                !answer.isNullOrBlank()
            }
        }
        if (hasAnswer) answeredCount++
    }
    return answeredCount
}

private fun calculateCorrectAnswers(state: MiniGamePlayState): Int {
    var correctCount = 0
    state.questions.forEach { question ->
        val questionOptions = state.questionOptions[question.id] ?: emptyList()
        
        val isCorrect = when (question.questionType) {
            com.example.datn.domain.models.QuestionType.SINGLE_CHOICE -> {
                val userAnswer = state.answers[question.id]
                val correctOption = questionOptions.find { it.isCorrect }
                userAnswer == correctOption?.id
            }
            com.example.datn.domain.models.QuestionType.MULTIPLE_CHOICE -> {
                val selectedIds = state.multipleChoiceAnswers[question.id] ?: emptySet()
                val correctIds = questionOptions.filter { it.isCorrect }.map { it.id }.toSet()
                selectedIds == correctIds && selectedIds.isNotEmpty()
            }
            com.example.datn.domain.models.QuestionType.FILL_BLANK -> {
                val userAnswer = state.answers[question.id]
                val correctOption = questionOptions.find { it.isCorrect }
                isTextEqual(userAnswer, correctOption?.content)
            }
            com.example.datn.domain.models.QuestionType.ESSAY -> {
                val userAnswer = state.answers[question.id]
                !userAnswer.isNullOrBlank()
            }
        }
        
        if (isCorrect) correctCount++
    }
    return correctCount
}

@Composable
private fun SingleChoiceOptions(
    options: List<com.example.datn.domain.models.MiniGameOption>,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    isSubmitted: Boolean
) {
    options.forEach { option ->
        val isSelected = selectedAnswer == option.id
        val isCorrect = option.isCorrect
        
        val backgroundColor = when {
            isSubmitted && isCorrect -> MaterialTheme.colorScheme.primaryContainer
            isSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            onClick = { 
                if (!isSubmitted) onAnswerSelected(option.id) 
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    enabled = !isSubmitted
                )
                
                Text(
                    text = option.content,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                
                if (isSubmitted) {
                    when {
                        isCorrect -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Correct",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        isSelected && !isCorrect -> Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Incorrect",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultipleChoiceOptions(
    options: List<com.example.datn.domain.models.MiniGameOption>,
    selectedAnswers: Set<String>,
    onAnswerToggled: (String) -> Unit,
    isSubmitted: Boolean
) {
    options.forEach { option ->
        val isSelected = selectedAnswers.contains(option.id)
        val isCorrect = option.isCorrect
        
        val backgroundColor = when {
            isSubmitted && isCorrect -> MaterialTheme.colorScheme.primaryContainer
            isSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            onClick = { 
                if (!isSubmitted) onAnswerToggled(option.id) 
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    enabled = !isSubmitted
                )
                
                Text(
                    text = option.content,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                
                if (isSubmitted) {
                    when {
                        isCorrect -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Correct",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        isSelected && !isCorrect -> Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Incorrect",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FillBlankQuestion(
    selectedAnswer: String?,
    onAnswerChanged: (String) -> Unit,
    isSubmitted: Boolean,
    correctAnswer: String?
) {
    val isCorrect = isSubmitted && isTextEqual(selectedAnswer, correctAnswer)
    
    OutlinedTextField(
        value = selectedAnswer ?: "",
        onValueChange = { if (!isSubmitted) onAnswerChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Your answer") },
        enabled = !isSubmitted,
        readOnly = isSubmitted,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isCorrect) {
                MaterialTheme.colorScheme.primary
            } else if (isSubmitted) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    )
    
    if (isSubmitted && correctAnswer != null && !isCorrect) {
        Text(
            text = "Correct answer: $correctAnswer",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Normalize text for comparison - convert to lowercase and trim whitespace
 * This ensures case-insensitive comparison for all text-based answers
 */
private fun normalizeText(text: String?): String {
    return text?.trim()?.lowercase() ?: ""
}

/**
 * Check if two texts are equal after normalization
 */
private fun isTextEqual(userText: String?, correctText: String?): Boolean {
    return normalizeText(userText) == normalizeText(correctText)
}

@Composable
private fun EssayQuestion(
    selectedAnswer: String?,
    onAnswerChanged: (String) -> Unit,
    isSubmitted: Boolean,
    expectedKeywords: String? = null
) {
    OutlinedTextField(
        value = selectedAnswer ?: "",
        onValueChange = { if (!isSubmitted) onAnswerChanged(it) },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        label = { Text("Your essay answer") },
        enabled = !isSubmitted,
        readOnly = isSubmitted,
        maxLines = 5,
        placeholder = { Text("Write your detailed answer here...") }
    )
    
    if (isSubmitted) {
        if (!expectedKeywords.isNullOrBlank()) {
            val hasKeywords = checkEssayKeywords(selectedAnswer, expectedKeywords)
            Text(
                text = if (hasKeywords) {
                    "✅ Your answer contains expected keywords"
                } else {
                    "💡 Expected keywords: $expectedKeywords"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (hasKeywords) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(
                text = "Essay answers require manual grading by the teacher.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Check if essay answer contains expected keywords
 */
private fun checkEssayKeywords(userAnswer: String?, expectedKeywords: String?): Boolean {
    if (userAnswer.isNullOrBlank() || expectedKeywords.isNullOrBlank()) return false
    
    val normalizedUserAnswer = normalizeText(userAnswer)
    val normalizedExpected = normalizeText(expectedKeywords)
    
    return normalizedUserAnswer.contains(normalizedExpected) || 
           normalizedExpected.split(" ").any { keyword -> 
               normalizedUserAnswer.contains(keyword.trim()) 
           }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
