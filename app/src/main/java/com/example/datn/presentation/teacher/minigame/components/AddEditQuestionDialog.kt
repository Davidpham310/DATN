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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.QuestionType
import com.example.datn.core.utils.validation.rules.minigame.ValidateQuestionContent
import com.example.datn.core.utils.validation.rules.minigame.ValidateQuestionOrder
import com.example.datn.core.utils.validation.rules.minigame.ValidateQuestionScore
import com.example.datn.core.utils.validation.rules.minigame.ValidateQuestionTimeLimit
import com.example.datn.core.utils.validation.rules.lesson.ValidateLessonOrderText
import kotlin.math.ceil
import java.util.Locale

@Composable
fun AddEditQuestionDialog(
    question: MiniGameQuestion?,
    gameId: String,
    onDismiss: () -> Unit,
    onConfirmAdd: (gameId: String, content: String, questionType: QuestionType, score: Double, timeLimit: Long, order: Int) -> Unit,
    onConfirmEdit: (id: String, gameId: String, content: String, questionType: QuestionType, score: Double, timeLimit: Long, order: Int) -> Unit
) {
    val isEditing = question != null

    val allowedQuestionTypes = remember {
        QuestionType.entries
    }

    // Ensure selected type is always compatible
    val initialQuestionType = remember(allowedQuestionTypes, question) {
        val existing = question?.questionType
        when {
            existing != null && existing in allowedQuestionTypes -> existing
            else -> allowedQuestionTypes.firstOrNull() ?: QuestionType.SINGLE_CHOICE
        }
    }

    var content by remember { mutableStateOf(question?.content ?: "") }
    var selectedQuestionType by remember(allowedQuestionTypes, question) { mutableStateOf(initialQuestionType) }
    var scoreText by remember {
        mutableStateOf(
            String.format(Locale.US, "%.1f", ceil((question?.score ?: 1.0) * 10.0) / 10.0)
        )
    }
    var timeLimit by remember { mutableStateOf(question?.timeLimit ?: 30L) }
    var orderText by remember { mutableStateOf(((question?.order ?: 0) + 1).toString()) }

    val contentValidator = remember { ValidateQuestionContent() }
    val scoreValidator = remember { ValidateQuestionScore() }
    val timeLimitValidator = remember { ValidateQuestionTimeLimit() }
    val orderTextValidator = remember { ValidateLessonOrderText() }
    val orderValidator = remember { ValidateQuestionOrder() }

    var contentError by remember { mutableStateOf<String?>(null) }
    var scoreError by remember { mutableStateOf<String?>(null) }
    var timeLimitError by remember { mutableStateOf<String?>(null) }
    var orderError by remember { mutableStateOf<String?>(null) }
    var isQuestionTypeExpanded by remember { mutableStateOf(false) }

    fun sanitizeScoreText(input: String): String {
        val normalized = input.replace(',', '.')
        val filtered = buildString {
            for (c in normalized) {
                if (c.isDigit() || c == '.') append(c)
            }
        }

        val firstDotIndex = filtered.indexOf('.')
        val withoutExtraDots = if (firstDotIndex == -1) {
            filtered
        } else {
            val before = filtered.substring(0, firstDotIndex)
            val after = filtered.substring(firstDotIndex + 1).replace(".", "")
            "$before.$after"
        }

        val parts = withoutExtraDots.split('.', limit = 2)
        return if (parts.size == 2) {
            parts[0] + "." + parts[1].take(1)
        } else {
            withoutExtraDots
        }
    }

    fun ceilTo1Decimal(value: Double): Double = ceil(value * 10.0) / 10.0

    fun validateFields(): Boolean {
        val result = contentValidator.validate(content)
        contentError = if (!result.successful) result.errorMessage else null

        val parsedScore = scoreText.trim().replace(',', '.').toDoubleOrNull()
        val roundedScore = parsedScore?.let { ceilTo1Decimal(it) }
        val scoreResult = if (roundedScore == null) {
            null
        } else {
            scoreValidator.validate(roundedScore)
        }
        scoreError = when {
            roundedScore == null -> "Điểm số không hợp lệ"
            scoreResult != null && !scoreResult.successful -> scoreResult.errorMessage
            else -> null
        }

        val timeResult = timeLimitValidator.validate(timeLimit)
        timeLimitError = if (!timeResult.successful) timeResult.errorMessage else null

        val orderTextResult = orderTextValidator.validate(orderText)
        if (!orderTextResult.successful) {
            orderError = orderTextResult.errorMessage
        } else {
            val desiredOrder = orderText.trim().toInt() - 1
            val orderResult = orderValidator.validate(desiredOrder)
            orderError = if (!orderResult.successful) orderResult.errorMessage else null
        }

        return contentError == null && scoreError == null && timeLimitError == null && orderError == null
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
                if (isEditing) {
                    OutlinedTextField(
                        value = selectedQuestionType.displayName,
                        onValueChange = {},
                        label = { Text("Loại câu hỏi *") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
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
                }

                // Score
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = {
                        scoreText = sanitizeScoreText(it)
                        if (scoreError != null) scoreError = null
                    },
                    label = { Text("Điểm số *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = scoreError != null,
                    supportingText = {
                        scoreError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
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
                        if (timeLimitError != null) timeLimitError = null
                    },
                    label = { Text("Thời gian (giây)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = timeLimitError != null,
                    supportingText = {
                        timeLimitError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Order (1-based UI)
                OutlinedTextField(
                    value = orderText,
                    onValueChange = {
                        orderText = it
                        if (orderError != null) orderError = null
                    },
                    label = { Text("Thứ tự *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = orderError != null,
                    supportingText = {
                        if (orderError != null) {
                            Text(orderError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(
                                text = "Thứ tự hiển thị trong danh sách câu hỏi",
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
                        val rawScore = scoreText.trim().replace(',', '.').toDoubleOrNull() ?: 1.0
                        val parsedScore = ceilTo1Decimal(rawScore)
                        scoreText = String.format(Locale.US, "%.1f", parsedScore)
                        val order = orderText.trim().toInt() - 1
                        if (isEditing) {
                            onConfirmEdit(
                                question!!.id,
                                gameId,
                                content,
                                selectedQuestionType,
                                parsedScore,
                                timeLimit,
                                order
                            )
                        } else {
                            onConfirmAdd(
                                gameId,
                                content,
                                selectedQuestionType,
                                parsedScore,
                                timeLimit,
                                order
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