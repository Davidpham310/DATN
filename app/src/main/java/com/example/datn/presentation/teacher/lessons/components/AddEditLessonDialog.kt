package com.example.datn.presentation.teacher.lessons.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.Lesson
import java.time.format.DateTimeFormatter

@Composable
fun AddEditLessonDialog(
    lesson: Lesson?,
    classId: String,
    onDismiss: () -> Unit,
    onConfirmAdd: (title: String, description: String?, contentLink: String?, order: Int) -> Unit,
    onConfirmEdit: (id: String, classId: String, title: String, description: String?, contentLink: String?, order: Int) -> Unit
) {
    var title by remember { mutableStateOf(lesson?.title ?: "") }
    var description by remember { mutableStateOf(lesson?.description ?: "") }
    var contentLink by remember { mutableStateOf(lesson?.contentLink ?: "") }
    var orderText by remember { mutableStateOf(lesson?.order?.toString() ?: "1") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var orderError by remember { mutableStateOf<String?>(null) }

    fun validateFields(): Boolean {
        titleError = when {
            title.isBlank() -> "Tiêu đề không được để trống"
            title.length < 3 -> "Tiêu đề phải có ít nhất 3 ký tự"
            else -> null
        }

        val orderValue = orderText.toIntOrNull()
        orderError = when {
            orderValue == null -> "Thứ tự phải là số"
            orderValue < 1 -> "Thứ tự phải lớn hơn 0"
            else -> null
        }

        return titleError == null && orderError == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (lesson == null) "Thêm bài học" else "Chỉnh sửa bài học",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tiêu đề
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError != null) titleError = null
                    },
                    label = { Text("Tiêu đề bài học *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = titleError != null,
                    supportingText = {
                        if (titleError != null) {
                            Text(
                                text = titleError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // Mô tả
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Link nội dung
                OutlinedTextField(
                    value = contentLink,
                    onValueChange = { contentLink = it },
                    label = { Text("Link tài liệu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "URL đến tài liệu, video hoặc tài nguyên học tập",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                // Thứ tự
                OutlinedTextField(
                    value = orderText,
                    onValueChange = {
                        if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                            orderText = it
                            if (orderError != null) orderError = null
                        }
                    },
                    label = { Text("Thứ tự *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = orderError != null,
                    supportingText = {
                        if (orderError != null) {
                            Text(
                                text = orderError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Thứ tự hiển thị trong danh sách bài học",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        val order = orderText.toIntOrNull() ?: 1
                        val desc = description.ifBlank { null }
                        val link = contentLink.ifBlank { null }

                        if (lesson == null) {
                            onConfirmAdd(title, desc, link, order)
                        } else {
                            onConfirmEdit(lesson.id, classId, title, desc, link, order)
                        }
                    }
                }
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

