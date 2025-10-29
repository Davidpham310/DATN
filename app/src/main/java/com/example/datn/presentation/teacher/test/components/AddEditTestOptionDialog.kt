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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.TestOption

@Composable
fun AddEditTestOptionDialog(
    editing: TestOption?,
    onDismiss: () -> Unit,
    onConfirm: (content: String, isCorrect: Boolean, mediaUrl: String?) -> Unit
) {
    val title = if (editing == null) "Thêm đáp án" else "Chỉnh sửa đáp án"
    val contentState = remember { mutableStateOf(editing?.content ?: "") }
    val urlState = remember { mutableStateOf(editing?.mediaUrl ?: "") }
    val correctState = remember { mutableStateOf(editing?.isCorrect ?: false) }

    LaunchedEffect(editing?.id) {
        contentState.value = editing?.content ?: ""
        urlState.value = editing?.mediaUrl ?: ""
        correctState.value = editing?.isCorrect ?: false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = contentState.value,
                    onValueChange = { contentState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nội dung đáp án") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlState.value,
                    onValueChange = { urlState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Media URL (tuỳ chọn)") }
                )
                Spacer(Modifier.height(8.dp))
                RowCheckbox(
                    checked = correctState.value,
                    onCheckedChange = { correctState.value = it },
                    label = "Đáp án đúng"
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(contentState.value, correctState.value, urlState.value.ifBlank { null }) }) {
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


