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
import com.example.datn.domain.models.Test

@Composable
fun TestList(
    tests: List<Test>,
    onTestClick: (Test) -> Unit,
    onGradeClick: ((Test) -> Unit)? = null,
    onEdit: ((Test) -> Unit)? = null,
    onDelete: ((Test) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (tests.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Chưa có bài kiểm tra nào",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Nhấn nút + để thêm bài kiểm tra đầu tiên",
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
            items(tests, key = { it.id }) { test ->
                TestItem(
                    test = test,
                    onClick = { onTestClick(test) },
                    onGrade = onGradeClick?.let { { it(test) } },
                    onEdit = onEdit?.let { { it(test) } },
                    onDelete = onDelete?.let { { it(test) } }
                )
            }
        }
    }
}
