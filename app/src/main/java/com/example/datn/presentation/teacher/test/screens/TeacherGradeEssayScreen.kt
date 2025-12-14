package com.example.datn.presentation.teacher.test.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.teacher.test.viewmodel.TeacherGradeEssayEvent
import com.example.datn.presentation.teacher.test.viewmodel.TeacherGradeEssayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherGradeEssayScreen(
    testId: String,
    resultId: String,
    onNavigateBack: () -> Unit,
    viewModel: TeacherGradeEssayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(testId, resultId) {
        viewModel.onEvent(TeacherGradeEssayEvent.Load(testId, resultId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chấm tự luận") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.onEvent(TeacherGradeEssayEvent.Submit) },
                        enabled = !state.isSubmitting
                    ) {
                        Text("Lưu điểm")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.essayAnswers.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Không có câu tự luận để chấm",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.essayAnswers, key = { it.question.id }) { item ->
                            val input = state.scoreInputs[item.question.id].orEmpty()
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Câu ${item.question.order}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = item.question.content)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Trả lời của học sinh:",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = item.answer.answer)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = input,
                                        onValueChange = { v ->
                                            viewModel.onEvent(TeacherGradeEssayEvent.ChangeScore(item.question.id, v))
                                        },
                                        label = { Text("Điểm (tối đa ${item.question.score})") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
