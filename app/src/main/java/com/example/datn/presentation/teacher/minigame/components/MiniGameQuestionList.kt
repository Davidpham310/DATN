package com.example.datn.presentation.teacher.minigame.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.MiniGameQuestion

@Composable
fun MiniGameQuestionList(
    questions: List<MiniGameQuestion>,
    questionOptions: Map<String, List<MiniGameOption>>,
    onEdit: (MiniGameQuestion) -> Unit,
    onDelete: (MiniGameQuestion) -> Unit,
    onClick: (MiniGameQuestion) -> Unit,
    modifier: Modifier = Modifier
) {
    if (questions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Quiz,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chưa có câu hỏi nào",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nhấn nút + để thêm câu hỏi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(questions.sortedBy { it.order }) { question ->
                MiniGameQuestionItem(
                    question = question,
                    options = questionOptions[question.id] ?: emptyList(),
                    onEdit = { onEdit(question) },
                    onDelete = { onDelete(question) },
                    onClick = { onClick(question) }
                )
            }
        }
    }
}
