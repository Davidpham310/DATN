package com.example.datn.presentation.teacher.lessons.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.LessonContent

@Composable
fun LessonContentList(
    lessonContents: List<LessonContent>,
    contentUrls: Map<String, String>,
    onEdit: (LessonContent) -> Unit,
    onDelete: (LessonContent) -> Unit,
    onClick: (LessonContent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lessonContents.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description, // Sử dụng icon mô tả/nội dung
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chưa có nội dung nào",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nhấn nút + để thêm nội dung mới cho bài học này",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Sử dụng LazyColumn để hiển thị danh sách hiệu quả
        LazyColumn(modifier = modifier.fillMaxSize()) {
            // Sắp xếp theo trường 'order' (giả định LessonContent có trường này)
            items(lessonContents.sortedBy { it.order }) { content ->
                LessonContentItem(
                    content = content,
                    contentUrl = contentUrls[content.id] ?: content.content,
                    onEdit = { onEdit(content) },
                    onDelete = { onDelete(content) },
                    onClick = { onClick(content) }
                )
            }
        }
    }
}