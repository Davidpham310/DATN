package com.example.datn.presentation.teacher.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.datn.core.utils.validation.rules.test.ValidateTestMediaUrl
import com.example.datn.core.utils.validation.rules.test.ValidateTestDisplayOrder
import com.example.datn.core.utils.validation.rules.test.ValidateTestQuestionContent
import com.example.datn.core.utils.validation.rules.test.ValidateTestQuestionScoreText
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTestQuestionDialog(
    testQuestion: TestQuestion? = null,
    onDismiss: () -> Unit,
    onConfirm: (content: String, score: Double, timeLimit: Int, order: Int, questionType: QuestionType, mediaUrl: String?) -> Unit
) {
    var content by remember { mutableStateOf(testQuestion?.content ?: "") }
    var score by remember { mutableStateOf(testQuestion?.score?.toString() ?: "") }
    var timeLimit by remember { mutableStateOf(testQuestion?.timeLimit?.toString() ?: "") }
    var displayOrder by remember { mutableStateOf(testQuestion?.order?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(testQuestion?.questionType ?: QuestionType.SINGLE_CHOICE) }
    var mediaUrl by remember { mutableStateOf(testQuestion?.mediaUrl ?: "") }
    var expanded by remember { mutableStateOf(false) }

    val contentValidator = remember { ValidateTestQuestionContent() }
    val scoreValidator = remember { ValidateTestQuestionScoreText() }
    val mediaUrlValidator = remember { ValidateTestMediaUrl() }
    val displayOrderValidator = remember { ValidateTestDisplayOrder() }

    var contentError by remember { mutableStateOf<String?>(null) }
    var scoreError by remember { mutableStateOf<String?>(null) }
    var timeLimitError by remember { mutableStateOf<String?>(null) }
    var displayOrderError by remember { mutableStateOf<String?>(null) }
    var mediaUrlError by remember { mutableStateOf<String?>(null) }

    // Sử dụng các QuestionType có sẵn trong dự án
    val questionTypes = listOf(
        QuestionType.SINGLE_CHOICE,
        QuestionType.MULTIPLE_CHOICE,
        QuestionType.FILL_BLANK,
        QuestionType.ESSAY
    )

    fun validateFields(): Boolean {
        val contentResult = contentValidator.validate(content)
        contentError = if (!contentResult.successful) contentResult.errorMessage else null

        val scoreResult = scoreValidator.validate(score)
        scoreError = if (!scoreResult.successful) scoreResult.errorMessage else null

        val timeLimitValue = timeLimit.trim().toIntOrNull() ?: 0
        val timeLimitResult = displayOrderValidator.validate(timeLimitValue)
        timeLimitError = if (!timeLimitResult.successful) timeLimitResult.errorMessage else null

        val displayOrderValue = displayOrder.trim().toIntOrNull() ?: 0
        val displayOrderResult = displayOrderValidator.validate(displayOrderValue)
        displayOrderError = if (!displayOrderResult.successful) displayOrderResult.errorMessage else null

        val mediaUrlResult = mediaUrlValidator.validate(mediaUrl.ifBlank { null })
        mediaUrlError = if (!mediaUrlResult.successful) mediaUrlResult.errorMessage else null

        return contentError == null && scoreError == null && timeLimitError == null && displayOrderError == null && mediaUrlError == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (testQuestion == null) "Thêm câu hỏi" else "Sửa câu hỏi") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        if (contentError != null) contentError = null
                    },
                    label = { Text("Nội dung câu hỏi *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = contentError != null,
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        contentError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Score
                OutlinedTextField(
                    value = score,
                    onValueChange = {
                        score = it
                        if (scoreError != null) scoreError = null
                    },
                    label = { Text("Điểm số *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = scoreError != null,
                    supportingText = {
                        scoreError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                OutlinedTextField(
                    value = timeLimit,
                    onValueChange = {
                        timeLimit = it
                        if (timeLimitError != null) timeLimitError = null
                    },
                    label = { Text("Thời gian trả lời *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = timeLimitError != null,
                    supportingText = {
                        timeLimitError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                OutlinedTextField(
                    value = displayOrder,
                    onValueChange = {
                        displayOrder = it
                        if (displayOrderError != null) displayOrderError = null
                    },
                    label = { Text("Thứ tự hiển thị *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = displayOrderError != null,
                    supportingText = {
                        displayOrderError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Question Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loại câu hỏi") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        questionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Media URL (optional)
                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = {
                        mediaUrl = it
                        if (mediaUrlError != null) mediaUrlError = null
                    },
                    label = { Text("URL hình ảnh/video (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mediaUrlError != null,
                    supportingText = {
                        mediaUrlError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        val timeLimitValue = timeLimit.trim().toIntOrNull() ?: 0
                        val displayOrderValue = displayOrder.trim().toIntOrNull() ?: 0
                        onConfirm(
                            content.trim(),
                            score.trim().toDouble(),
                            timeLimitValue,
                            displayOrderValue,
                            selectedType,
                            mediaUrl.trim().ifBlank { null }
                        )
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
