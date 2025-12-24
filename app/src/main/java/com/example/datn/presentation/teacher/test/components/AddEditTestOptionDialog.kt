package com.example.datn.presentation.teacher.test.components

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.core.utils.validation.rules.test.ValidateTestDisplayOrder
import com.example.datn.core.utils.validation.rules.test.ValidateTestMediaUrl
import com.example.datn.core.utils.validation.rules.test.ValidateTestOptionContent
import com.example.datn.core.utils.validation.rules.test.ValidateTestOptionCorrectness
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestOption

@Composable
fun AddEditTestOptionDialog(
    editing: TestOption?,
    questionType: QuestionType?,
    onDismiss: () -> Unit,
    onConfirm: (content: String, isCorrect: Boolean, order: Int, mediaUrl: String?) -> Unit
) {
    val title = if (editing == null) "Thêm đáp án" else "Chỉnh sửa đáp án"
    val contentState = remember { mutableStateOf(editing?.content ?: "") }
    val orderState = remember { mutableStateOf(editing?.order?.toString() ?: "") }
    val urlState = remember { mutableStateOf(editing?.mediaUrl ?: "") }
    val correctState = remember { mutableStateOf(editing?.isCorrect ?: false) }

    val contentValidator = remember { ValidateTestOptionContent() }
    val displayOrderValidator = remember { ValidateTestDisplayOrder() }
    val mediaUrlValidator = remember { ValidateTestMediaUrl() }
    val correctnessValidator = remember { ValidateTestOptionCorrectness() }

    var contentError by remember { mutableStateOf<String?>(null) }
    var orderError by remember { mutableStateOf<String?>(null) }
    var mediaUrlError by remember { mutableStateOf<String?>(null) }
    var correctnessError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editing?.id) {
        contentState.value = editing?.content ?: ""
        orderState.value = editing?.order?.toString() ?: ""
        urlState.value = editing?.mediaUrl ?: ""
        correctState.value = editing?.isCorrect ?: false
    }

    val qType = questionType
    val showCorrectField = qType == QuestionType.SINGLE_CHOICE || qType == QuestionType.MULTIPLE_CHOICE

    LaunchedEffect(qType, editing?.id) {
        when (qType) {
            QuestionType.FILL_BLANK -> correctState.value = true
            QuestionType.ESSAY -> correctState.value = false
            else -> {}
        }
    }

    fun validateFields(isCorrectForSubmit: Boolean): Boolean {
        val contentResult = contentValidator.validate(contentState.value)
        contentError = if (!contentResult.successful) contentResult.errorMessage else null

        val orderValue = orderState.value.trim().toIntOrNull() ?: 0
        val orderResult = displayOrderValidator.validate(orderValue)
        orderError = if (!orderResult.successful) orderResult.errorMessage else null

        val mediaUrlResult = mediaUrlValidator.validate(urlState.value.ifBlank { null })
        mediaUrlError = if (!mediaUrlResult.successful) mediaUrlResult.errorMessage else null

        val correctnessResult = correctnessValidator.validate(qType to isCorrectForSubmit)
        correctnessError = if (!correctnessResult.successful) correctnessResult.errorMessage else null

        return contentError == null && orderError == null && mediaUrlError == null && correctnessError == null
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
                    value = orderState.value,
                    onValueChange = {
                        orderState.value = it
                        if (orderError != null) orderError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Thứ tự hiển thị") },
                    isError = orderError != null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    supportingText = {
                        orderError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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

                if (showCorrectField) {
                    RowCheckbox(
                        checked = correctState.value,
                        onCheckedChange = { correctState.value = it },
                        label = "Đáp án đúng"
                    )
                }

                correctnessError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isCorrectForSubmit = when (qType) {
                        QuestionType.FILL_BLANK -> true
                        QuestionType.ESSAY -> false
                        else -> correctState.value
                    }
                    val orderValue = orderState.value.trim().toIntOrNull() ?: 0
                    if (validateFields(isCorrectForSubmit)) {
                        onConfirm(
                            contentState.value.trim(),
                            isCorrectForSubmit,
                            orderValue,
                            urlState.value.trim().ifBlank { null }
                        )
                    }
                }
            ) {
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
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}


