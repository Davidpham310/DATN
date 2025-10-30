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
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTestQuestionDialog(
    testQuestion: TestQuestion? = null,
    onDismiss: () -> Unit,
    onConfirm: (content: String, score: Double, questionType: QuestionType, mediaUrl: String?) -> Unit
) {
    var content by remember { mutableStateOf(testQuestion?.content ?: "") }
    var score by remember { mutableStateOf(testQuestion?.score?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(testQuestion?.questionType ?: QuestionType.SINGLE_CHOICE) }
    var mediaUrl by remember { mutableStateOf(testQuestion?.mediaUrl ?: "") }
    var expanded by remember { mutableStateOf(false) }

    // Sử dụng các QuestionType có sẵn trong dự án
    val questionTypes = listOf(
        QuestionType.SINGLE_CHOICE,
        QuestionType.MULTIPLE_CHOICE,
        QuestionType.FILL_BLANK,
        QuestionType.ESSAY
    )

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
                    onValueChange = { content = it },
                    label = { Text("Nội dung câu hỏi *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Score
                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text("Điểm số *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                    onValueChange = { mediaUrl = it },
                    label = { Text("URL hình ảnh/video (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val scoreValue = score.toDoubleOrNull()
                    if (content.isNotBlank() && scoreValue != null && scoreValue > 0) {
                        onConfirm(
                            content.trim(),
                            scoreValue,
                            selectedType,
                            mediaUrl.ifBlank { null }
                        )
                    }
                },
                enabled = content.isNotBlank() && score.toDoubleOrNull() != null && (score.toDoubleOrNull() ?: 0.0) > 0
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
