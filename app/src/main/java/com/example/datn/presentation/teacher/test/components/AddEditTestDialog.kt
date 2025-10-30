package com.example.datn.presentation.teacher.test.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AddEditTestDialog(
    test: Test?,
    classId: String,
    lessonId: String,
    onDismiss: () -> Unit,
    onConfirmAdd: (
        classId: String,
        lessonId: String,
        title: String,
        description: String?,
        totalScore: Double,
        startTime: Instant,
        endTime: Instant
    ) -> Unit,
    onConfirmEdit: (
        id: String,
        classId: String,
        lessonId: String,
        title: String,
        description: String?,
        totalScore: Double,
        startTime: Instant,
        endTime: Instant
    ) -> Unit
) {
    var title by remember { mutableStateOf(test?.title ?: "") }
    var description by remember { mutableStateOf(test?.description ?: "") }
    var totalScoreText by remember { mutableStateOf(test?.totalScore?.toString() ?: "100") }
    
    // Format datetime for display
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    val zoneId = ZoneId.systemDefault()
    
    var startTimeText by remember {
        mutableStateOf(
            test?.startTime?.atZone(zoneId)?.toLocalDateTime()?.format(dateTimeFormatter)
                ?: LocalDateTime.now().format(dateTimeFormatter)
        )
    }
    var endTimeText by remember {
        mutableStateOf(
            test?.endTime?.atZone(zoneId)?.toLocalDateTime()?.format(dateTimeFormatter)
                ?: LocalDateTime.now().plusHours(2).format(dateTimeFormatter)
        )
    }

    var titleError by remember { mutableStateOf<String?>(null) }
    var totalScoreError by remember { mutableStateOf<String?>(null) }
    var startTimeError by remember { mutableStateOf<String?>(null) }
    var endTimeError by remember { mutableStateOf<String?>(null) }

    fun validateFields(): Boolean {
        titleError = when {
            title.isBlank() -> "Tiêu đề không được để trống"
            title.length < 3 -> "Tiêu đề phải có ít nhất 3 ký tự"
            else -> null
        }

        val scoreValue = totalScoreText.toDoubleOrNull()
        totalScoreError = when {
            scoreValue == null -> "Tổng điểm phải là số"
            scoreValue <= 0 -> "Tổng điểm phải lớn hơn 0"
            else -> null
        }

        var startTime: Instant? = null
        var endTime: Instant? = null

        try {
            startTime = LocalDateTime.parse(startTimeText, dateTimeFormatter)
                .atZone(zoneId)
                .toInstant()
            startTimeError = null
        } catch (e: Exception) {
            startTimeError = "Định dạng thời gian không hợp lệ (yyyy-MM-dd'T'HH:mm)"
        }

        try {
            endTime = LocalDateTime.parse(endTimeText, dateTimeFormatter)
                .atZone(zoneId)
                .toInstant()
            endTimeError = null
        } catch (e: Exception) {
            endTimeError = "Định dạng thời gian không hợp lệ (yyyy-MM-dd'T'HH:mm)"
        }

        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            startTimeError = "Thời gian bắt đầu phải trước thời gian kết thúc"
            endTimeError = "Thời gian kết thúc phải sau thời gian bắt đầu"
        }

        return titleError == null && totalScoreError == null && 
               startTimeError == null && endTimeError == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (test == null) "Thêm bài kiểm tra" else "Chỉnh sửa bài kiểm tra",
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
                // Tiêu đề
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError != null) titleError = null
                    },
                    label = { Text("Tiêu đề bài kiểm tra *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = titleError != null,
                    supportingText = {
                        if (titleError != null) {
                            Text(
                                text = titleError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // Mô tả
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Tổng điểm
                OutlinedTextField(
                    value = totalScoreText,
                    onValueChange = {
                        totalScoreText = it
                        if (totalScoreError != null) totalScoreError = null
                    },
                    label = { Text("Tổng điểm *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = totalScoreError != null,
                    supportingText = {
                        if (totalScoreError != null) {
                            Text(
                                text = totalScoreError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Điểm tối đa của bài kiểm tra",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // Thời gian bắt đầu
                OutlinedTextField(
                    value = startTimeText,
                    onValueChange = {
                        startTimeText = it
                        if (startTimeError != null) startTimeError = null
                    },
                    label = { Text("Thời gian bắt đầu *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = startTimeError != null,
                    supportingText = {
                        if (startTimeError != null) {
                            Text(
                                text = startTimeError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Định dạng: yyyy-MM-dd'T'HH:mm (VD: 2024-01-15T09:00)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                // Thời gian kết thúc
                OutlinedTextField(
                    value = endTimeText,
                    onValueChange = {
                        endTimeText = it
                        if (endTimeError != null) endTimeError = null
                    },
                    label = { Text("Thời gian kết thúc *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = endTimeError != null,
                    supportingText = {
                        if (endTimeError != null) {
                            Text(
                                text = endTimeError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                text = "Định dạng: yyyy-MM-dd'T'HH:mm (VD: 2024-01-15T11:00)",
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
                        val totalScore = totalScoreText.toDouble()
                        val startTime = LocalDateTime.parse(startTimeText, dateTimeFormatter)
                            .atZone(zoneId)
                            .toInstant()
                        val endTime = LocalDateTime.parse(endTimeText, dateTimeFormatter)
                            .atZone(zoneId)
                            .toInstant()
                        val desc = description.ifBlank { null }

                        if (test == null) {
                            onConfirmAdd(classId, lessonId, title, desc, totalScore, startTime, endTime)
                        } else {
                            onConfirmEdit(test.id, classId, lessonId, title, desc, totalScore, startTime, endTime)
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
