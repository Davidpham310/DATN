package com.example.datn.presentation.teacher.minigame.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.core.utils.validation.rules.minigame.ValidateMatchingPair
import com.example.datn.core.utils.validation.rules.minigame.ValidateOptionContent
import com.example.datn.core.utils.validation.rules.minigame.ValidateOptionHint
import com.example.datn.core.utils.validation.rules.minigame.ValidateOptionMediaUrl
import com.example.datn.core.utils.validation.rules.minigame.ValidateOptionPairContent
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.QuestionType

@Composable
fun AddEditMiniGameOptionDialog(
    editing: MiniGameOption?,
    questionType: QuestionType?,
    onDismiss: () -> Unit,
    onConfirm: (content: String, isCorrect: Boolean, mediaUrl: String?, hint: String?, pairContent: String?) -> Unit
) {
    val title = if (editing == null) "Thêm đáp án" else "Chỉnh sửa đáp án"
    val contentState = remember { mutableStateOf(editing?.content ?: "") }
    val urlState = remember { mutableStateOf(editing?.mediaUrl ?: "") }
    val correctState = remember { mutableStateOf(editing?.isCorrect ?: false) }
    val hintState = remember { mutableStateOf(editing?.hint ?: "") }
    val pairContentState = remember { mutableStateOf(editing?.pairContent ?: "") }

    val contentValidator = remember { ValidateOptionContent() }
    val mediaUrlValidator = remember { ValidateOptionMediaUrl() }
    val hintValidator = remember { ValidateOptionHint() }
    val pairContentValidator = remember { ValidateOptionPairContent() }
    val matchingPairValidator = remember { ValidateMatchingPair() }

    var contentError by remember { mutableStateOf<String?>(null) }
    var mediaUrlError by remember { mutableStateOf<String?>(null) }
    var hintError by remember { mutableStateOf<String?>(null) }
    var pairContentError by remember { mutableStateOf<String?>(null) }
    
    // Determine if we need special fields
    val qType = questionType
    val showHintField = qType == QuestionType.FILL_BLANK
    val showCorrectField = qType == QuestionType.SINGLE_CHOICE || qType == QuestionType.MULTIPLE_CHOICE
    val showPairContentField = false

    fun validateFields(): Boolean {
        val contentResult = contentValidator.validate(contentState.value)
        contentError = if (!contentResult.successful) contentResult.errorMessage else null

        val mediaUrlResult = mediaUrlValidator.validate(urlState.value)
        mediaUrlError = if (!mediaUrlResult.successful) mediaUrlResult.errorMessage else null

        if (showPairContentField) {
            val pairContentResult = pairContentValidator.validate(pairContentState.value.ifBlank { null })
            pairContentError = if (!pairContentResult.successful) pairContentResult.errorMessage else null

            val matchingPairResult = matchingPairValidator.validate(contentState.value to pairContentState.value.ifBlank { null })
            if (!matchingPairResult.successful) {
                pairContentError = matchingPairResult.errorMessage
            }
        } else {
            pairContentError = null
        }

        if (showHintField) {
            val hintResult = hintValidator.validate(hintState.value.ifBlank { null })
            hintError = if (!hintResult.successful) hintResult.errorMessage else null
        } else {
            hintError = null
        }

        return contentError == null && mediaUrlError == null && hintError == null && pairContentError == null
    }

    LaunchedEffect(editing?.id) {
        contentState.value = editing?.content ?: ""
        urlState.value = editing?.mediaUrl ?: ""
        correctState.value = editing?.isCorrect ?: false
        hintState.value = editing?.hint ?: ""
        pairContentState.value = editing?.pairContent ?: ""
    }

    LaunchedEffect(qType, editing?.id) {
        when (qType) {
            QuestionType.FILL_BLANK -> correctState.value = true
            QuestionType.ESSAY -> correctState.value = false
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = contentState.value,
                    onValueChange = {
                        contentState.value = it
                        if (contentError != null) contentError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nội dung đáp án") },
                    isError = contentError != null,
                    supportingText = {
                        contentError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlState.value,
                    onValueChange = {
                        urlState.value = it
                        if (mediaUrlError != null) mediaUrlError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Media URL (tuỳ chọn)") },
                    isError = mediaUrlError != null,
                    supportingText = {
                        mediaUrlError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                )
                Spacer(Modifier.height(8.dp))
                
                // Hint field (primarily for fill-blank)
                if (showHintField) {
                    OutlinedTextField(
                        value = hintState.value,
                        onValueChange = {
                            hintState.value = it
                            if (hintError != null) hintError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Gợi ý (VD: a__le cho apple)") },
                        isError = hintError != null,
                        supportingText = {
                            hintError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            } ?: Text("Dùng _ để ẩn ký tự", style = MaterialTheme.typography.bodySmall)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                // Pair content field (used for matching-style questions)
                if (showPairContentField) {
                    OutlinedTextField(
                        value = pairContentState.value,
                        onValueChange = {
                            pairContentState.value = it
                            if (pairContentError != null) pairContentError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nội dung cặp ghép") },
                        isError = pairContentError != null,
                        supportingText = {
                            pairContentError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            } ?: Text("Nội dung để ghép với đáp án này (để trống nếu không dùng)", style = MaterialTheme.typography.bodySmall)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (showCorrectField) {
                    RowCheckbox(
                        checked = correctState.value,
                        onCheckedChange = { correctState.value = it },
                        label = "Đáp án đúng"
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (validateFields()) {
                    val isCorrectForSubmit = when (qType) {
                        QuestionType.FILL_BLANK -> true
                        QuestionType.ESSAY -> false
                        else -> correctState.value
                    }
                    onConfirm(
                        contentState.value,
                        isCorrectForSubmit,
                        urlState.value.ifBlank { null },
                        if (showHintField) hintState.value.ifBlank { null } else null,
                        if (showPairContentField) pairContentState.value.ifBlank { null } else null
                    )
                }
            }) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun RowCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}


