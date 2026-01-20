package com.example.datn.presentation.teacher.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.graphics.BitmapFactory
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
    onSelectFile: () -> Unit,
    onClearSelectedFile: () -> Unit,
    selectedFileName: String?,
    isLoading: Boolean = false,
    onConfirm: (content: String, score: Double, timeLimit: Int, order: Int, questionType: QuestionType, mediaUrl: String?) -> Unit,
    imagePreviewUri: Uri? = null
) {
    val isEditing = testQuestion != null

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

        return contentError == null && scoreError == null && timeLimitError == null && displayOrderError == null
    }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = { Text(if (testQuestion == null) "Thêm câu hỏi" else "Sửa câu hỏi") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
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
                    enabled = !isLoading,
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
                    enabled = !isLoading,
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
                    enabled = !isLoading,
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
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = displayOrderError != null,
                    supportingText = {
                        displayOrderError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Question Type Dropdown
                if (isEditing) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Loại câu hỏi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = {
                            if (!isLoading) expanded = !expanded
                        }
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
                                .menuAnchor(),
                            enabled = !isLoading
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
                }

                // Media URL (optional)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSelectFile,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chọn file")
                    }

                    if (!selectedFileName.isNullOrBlank()) {
                        IconButton(
                            onClick = onClearSelectedFile,
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Bỏ chọn file")
                        }
                    }
                }

                if (!selectedFileName.isNullOrBlank()) {
                    Text(
                        text = selectedFileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Preview image if available
                val context = LocalContext.current
                val previewBitmap = remember(imagePreviewUri) {
                    imagePreviewUri?.let { uri ->
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                if (previewBitmap != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                // Media URL text input removed. File selection above is the source of media.
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
                            null
                        )
                    }
                },
                enabled = !isLoading
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Xác nhận")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Hủy")
            }
        }
    )
}
