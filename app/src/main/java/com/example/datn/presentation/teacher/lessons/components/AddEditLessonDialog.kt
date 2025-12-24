package com.example.datn.presentation.teacher.lessons.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.datn.domain.models.Lesson
import com.example.datn.core.utils.validation.rules.lesson.ValidateLessonDescription
import com.example.datn.core.utils.validation.rules.lesson.ValidateLessonOrderText
import com.example.datn.core.utils.validation.rules.lesson.ValidateLessonTitle
import com.example.datn.presentation.common.dialogs.FormDialog

@Composable
fun AddEditLessonDialog(
    lesson: Lesson?,
    classId: String,
    onDismiss: () -> Unit,
    onConfirmAdd: (title: String, description: String?) -> Unit,
    onConfirmEdit: (id: String, classId: String, title: String, description: String?, order: Int) -> Unit
) {
    var title by remember { mutableStateOf(lesson?.title ?: "") }
    var description by remember { mutableStateOf(lesson?.description ?: "") }
    var orderText by remember { mutableStateOf(lesson?.order?.toString() ?: "1") }

    val titleValidator = remember { ValidateLessonTitle() }
    val descriptionValidator = remember { ValidateLessonDescription() }
    val orderValidator = remember { ValidateLessonOrderText() }

    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var orderError by remember { mutableStateOf<String?>(null) }

    fun validateFields(): Boolean {
        val titleResult = titleValidator.validate(title)
        titleError = if (!titleResult.successful) titleResult.errorMessage else null

        val desc = description.ifBlank { null }
        val descriptionResult = descriptionValidator.validate(desc)
        descriptionError = if (!descriptionResult.successful) descriptionResult.errorMessage else null

        val orderResult = orderValidator.validate(orderText)
        orderError = if (!orderResult.successful) orderResult.errorMessage else null

        return titleError == null && descriptionError == null && orderError == null
    }

    FormDialog(
        title = if (lesson == null) "Thêm bài học" else "Chỉnh sửa bài học",
        confirmText = "Xác nhận",
        onConfirm = {
            if (validateFields()) {
                val order = orderText.trim().toInt()
                val desc = description.ifBlank { null }

                if (lesson == null) {
                    onConfirmAdd(title, desc)
                } else {
                    onConfirmEdit(lesson.id, classId, title, desc, order)
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
            onValueChange = {
                description = it
                if (descriptionError != null) descriptionError = null
            },
            label = { Text("Mô tả") },
            modifier = Modifier.fillMaxWidth(),
            isError = descriptionError != null,
            minLines = 3,
            maxLines = 5,
            supportingText = {
                if (descriptionError != null) {
                    Text(
                        text = descriptionError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        // Thứ tự
        OutlinedTextField(
            value = orderText,
            onValueChange = {
                orderText = it
                if (orderError != null) orderError = null
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

