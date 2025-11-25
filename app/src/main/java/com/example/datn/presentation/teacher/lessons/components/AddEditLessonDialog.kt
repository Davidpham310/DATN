package com.example.datn.presentation.teacher.lessons.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.Lesson
import com.example.datn.core.utils.validation.rules.lesson.ValidateLessonTitle
import com.example.datn.presentation.common.dialogs.FormDialog
import java.time.format.DateTimeFormatter

@Composable
fun AddEditLessonDialog(
    lesson: Lesson?,
    classId: String,
    onDismiss: () -> Unit,
    onConfirmAdd: (title: String, description: String?, contentLink: String?) -> Unit,
    onConfirmEdit: (id: String, classId: String, title: String, description: String?, contentLink: String?, order: Int) -> Unit
) {
    var title by remember { mutableStateOf(lesson?.title ?: "") }
    var description by remember { mutableStateOf(lesson?.description ?: "") }
    var contentLink by remember { mutableStateOf(lesson?.contentLink ?: "") }
    var orderText by remember { mutableStateOf(lesson?.order?.toString() ?: "1") }

    val titleValidator = remember { ValidateLessonTitle() }

    var titleError by remember { mutableStateOf<String?>(null) }
    var orderError by remember { mutableStateOf<String?>(null) }

    fun validateFields(): Boolean {
        val titleResult = titleValidator.validate(title)
        titleError = if (!titleResult.successful) titleResult.errorMessage else null

        val orderValue = orderText.toIntOrNull()
        orderError = when {
            orderValue == null -> "Thứ tự phải là số"
            orderValue < 1 -> "Thứ tự phải lớn hơn 0"
            else -> null
        }

        return titleError == null && orderError == null
    }

    FormDialog(
        title = if (lesson == null) "Thêm bài học" else "Chỉnh sửa bài học",
        confirmText = "Xác nhận",
        onConfirm = {
            if (validateFields()) {
                val order = orderText.toIntOrNull() ?: 1
                val desc = description.ifBlank { null }
                val link = contentLink.ifBlank { null }

                if (lesson == null) {
                    onConfirmAdd(title, desc, link)
                } else {
                    onConfirmEdit(lesson.id, classId, title, desc, link, order)
                }
            }
        },
        onDismiss = onDismiss
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
}

