package com.example.datn.presentation.teacher.classes.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.Class

@Composable
fun AddEditClassDialog(
    classObj: Class?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, classCode: String) -> Unit
) {
    var name by remember { mutableStateOf(classObj?.name ?: "") }
    var classCode by remember { mutableStateOf(classObj?.classCode ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (classObj == null) "Thêm lớp" else "Sửa lớp") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên lớp") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OutlinedTextField(
                    value = classCode,
                    onValueChange = { classCode = it },
                    label = { Text("Mã lớp") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && classCode.isNotBlank()) {
                    onConfirm(name, classCode)
                }
            }) {
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
