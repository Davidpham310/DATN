package com.example.datn.presentation.teacher.classes.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.core.utils.validation.rules.classmanager.ValidateClassCode
import com.example.datn.core.utils.validation.rules.classmanager.ValidateClassName
import com.example.datn.core.utils.validation.rules.classmanager.ValidateGradeLevel
import com.example.datn.core.utils.validation.rules.classmanager.ValidateSubject
import com.example.datn.domain.models.Class

@Composable
fun AddEditClassDialog(
    classObj: Class?,
    onDismiss: () -> Unit,
    onConfirmAdd: (name: String, classCode: String, gradeLevel: Int, subject: String) -> Unit,
    onConfirmEdit: (id: String, name: String, classCode: String, gradeLevel: Int, subject: String) -> Unit
) {
    // State cho các trường input
    var name by remember { mutableStateOf(classObj?.name ?: "") }
    var classCode by remember { mutableStateOf(classObj?.classCode ?: "") }
    var gradeLevelText by remember { mutableStateOf(classObj?.gradeLevel?.toString() ?: "") }
    var subject by remember { mutableStateOf(classObj?.subject ?: "") }

    // State cho validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var classCodeError by remember { mutableStateOf<String?>(null) }
    var gradeLevelError by remember { mutableStateOf<String?>(null) }
    var subjectError by remember { mutableStateOf<String?>(null) }

    // Validators
    val validateName = remember { ValidateClassName() }
    val validateCode = remember { ValidateClassCode() }
    val validateGrade = remember { ValidateGradeLevel() }
    val validateSubject = remember { ValidateSubject() }

    // Hàm validate tất cả các trường
    fun validateAllFields(): Boolean {
        val nameResult = validateName.validate(name)
        val codeResult = validateCode.validate(classCode)
        val gradeResult = validateGrade.validate(gradeLevelText.toIntOrNull() ?: 0)
        val subjectResult = validateSubject.validate(subject)

        nameError = if (!nameResult.successful) nameResult.errorMessage else null
        classCodeError = if (!codeResult.successful) codeResult.errorMessage else null
        gradeLevelError = if (!gradeResult.successful) gradeResult.errorMessage else null
        subjectError = if (!subjectResult.successful) subjectResult.errorMessage else null

        return nameResult.successful &&
                codeResult.successful &&
                gradeResult.successful &&
                subjectResult.successful
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (classObj == null) "Thêm lớp học" else "Chỉnh sửa lớp học",
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
                // Tên lớp học
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        // Clear error khi user nhập
                        if (nameError != null) {
                            nameError = null
                        }
                    },
                    label = { Text("Tên lớp học") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(
                                text = nameError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // Mã lớp học
                OutlinedTextField(
                    value = classCode,
                    onValueChange = {
                        classCode = it
                        if (classCodeError != null) {
                            classCodeError = null
                        }
                    },
                    label = { Text("Mã lớp học") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = classCodeError != null,
                    supportingText = {
                        if (classCodeError != null) {
                            Text(
                                text = classCodeError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // Khối lớp
                OutlinedTextField(
                    value = gradeLevelText,
                    onValueChange = {
                        if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                            gradeLevelText = it
                            if (gradeLevelError != null) {
                                gradeLevelError = null
                            }
                        }
                    },
                    label = { Text("Khối lớp") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = gradeLevelError != null,
                    supportingText = {
                        if (gradeLevelError != null) {
                            Text(
                                text = gradeLevelError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Nhập số từ 1-12",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // Môn học
                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        if (subjectError != null) {
                            subjectError = null
                        }
                    },
                    label = { Text("Môn học") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = subjectError != null,
                    supportingText = {
                        if (subjectError != null) {
                            Text(
                                text = subjectError!!,
                                color = MaterialTheme.colorScheme.error,
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
                    // Validate tất cả các trường
                    if (validateAllFields()) {
                        val grade = gradeLevelText.toIntOrNull() ?: 1
                        if (classObj == null) {
                            onConfirmAdd(name, classCode, grade, subject)
                        } else {
                            onConfirmEdit(classObj.id, name, classCode, grade, subject)
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