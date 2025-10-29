package com.example.datn.presentation.teacher.test.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.TestQuestion
import com.example.datn.presentation.teacher.test.TestQuestionManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestQuestionManagerScreen(
    testId: String,
    testTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToOptions: (questionId: String, content: String) -> Unit,
    viewModel: TestQuestionManagerViewModel = hiltViewModel()
) {
    val state = viewModel.state

    LaunchedEffect(testId) {
        viewModel.setTest(testId, testTitle)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TopAppBar(title = { Text("Câu hỏi: $testTitle") })
        Text("Danh sách câu hỏi", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(state.value.questions, key = { it.id }) { q ->
                TestQuestionItem(question = q, onClick = { onNavigateToOptions(q.id, q.content) })
            }
        }
    }
}

@Composable
private fun TestQuestionItem(question: TestQuestion, onClick: () -> Unit) {
    Surface(tonalElevation = 1.dp, modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Column(Modifier.padding(12.dp)) {
            Text(question.content, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.padding(top = 4.dp))
            Text("Điểm: ${question.score}", style = MaterialTheme.typography.bodyMedium)
        }
    }
    Spacer(Modifier.padding(top = 8.dp))
}


