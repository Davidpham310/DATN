package com.example.datn.presentation.teacher.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.TestQuestion

@Composable
fun TestQuestionList(
    questions: List<TestQuestion>,
    onQuestionClick: (TestQuestion) -> Unit,
    onEdit: ((TestQuestion) -> Unit)? = null,
    onDelete: ((TestQuestion) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (questions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Chưa có câu hỏi nào",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Nhấn nút + để thêm câu hỏi đầu tiên",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(questions, key = { it.id }) { question ->
                TestQuestionItem(
                    question = question,
                    onClick = { onQuestionClick(question) },
                    onEdit = onEdit?.let { { it(question) } },
                    onDelete = onDelete?.let { { it(question) } }
                )
            }
        }
    }
}
