package com.example.datn.presentation.teacher.minigame.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.GameType
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.QuestionType

@Composable
fun AddEditQuestionDialog(
    question: MiniGameQuestion?,
    gameId: String,
    gameType: GameType?,
    onDismiss: () -> Unit,
    onConfirmAdd: (gameId: String, content: String, questionType: QuestionType, score: Double, timeLimit: Long) -> Unit,
    onConfirmEdit: (id: String, gameId: String, content: String, questionType: QuestionType, score: Double, timeLimit: Long) -> Unit
) {
    val isEditing = question != null

    // Get allowed question types based on game type
    val allowedQuestionTypes = remember(gameType) {
        gameType?.getAllowedQuestionTypes() ?: QuestionType.entries
    }

    // Set default question type to first allowed type
    val defaultQuestionType = remember(allowedQuestionTypes) {
        question?.questionType ?: allowedQuestionTypes.firstOrNull() ?: QuestionType.SINGLE_CHOICE
    }

    var content by remember { mutableStateOf(question?.content ?: "") }
    var selectedQuestionType by remember { mutableStateOf(defaultQuestionType) }
    var score by remember { mutableStateOf(question?.score ?: 1.0) }
    var timeLimit by remember { mutableStateOf(question?.timeLimit ?: 30L) }

    var contentError by remember { mutableStateOf<String?>(null) }
    var isQuestionTypeExpanded by remember { mutableStateOf(false) }

    fun validateFields(): Boolean {
        contentError = when {
            content.isBlank() -> "Nội dung câu hỏi không được để trống"
            content.length < 5 -> "Nội dung câu hỏi phải có ít nhất 5 ký tự"
            else -> null
        }
        return contentError == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Chỉnh sửa câu hỏi" else "Thêm câu hỏi",
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
                    supportingText = {
                        contentError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    minLines = 3,
                    maxLines = 5
                )

                // Question Type Dropdown
                Box {
                    OutlinedTextField(
                        value = selectedQuestionType.displayName,
                        onValueChange = {},
                        label = { Text("Loại câu hỏi *") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                Modifier.clickable { isQuestionTypeExpanded = !isQuestionTypeExpanded }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isQuestionTypeExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isQuestionTypeExpanded,
                        onDismissRequest = { isQuestionTypeExpanded = false }
                    ) {
                        allowedQuestionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedQuestionType = type
                                    isQuestionTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Score
                OutlinedTextField(
                    value = score.toString(),
                    onValueChange = { 
                        try {
                            score = it.toDoubleOrNull() ?: 1.0
                        } catch (e: Exception) {
                            // Keep current value if invalid
                        }
                    },
                    label = { Text("Điểm số") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Time Limit
                OutlinedTextField(
                    value = timeLimit.toString(),
                    onValueChange = { 
                        try {
                            timeLimit = it.toLongOrNull() ?: 30L
                        } catch (e: Exception) {
                            // Keep current value if invalid
                        }
                    },
                    label = { Text("Thời gian (giây)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        if (isEditing) {
                            onConfirmEdit(
                                question!!.id,
                                gameId,
                                content,
                                selectedQuestionType,
                                score,
                                timeLimit
                            )
                        } else {
                            onConfirmAdd(
                                gameId,
                                content,
                                selectedQuestionType,
                                score,
                                timeLimit
                            )
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