package com.example.datn.presentation.teacher.lessons.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import com.example.datn.core.utils.validation.rules.lesson.ValidateLessonTitle
import com.example.datn.core.utils.validation.rules.lesson.ValidateLessonContentFile
import com.example.datn.core.utils.validation.rules.lesson.LessonContentFileValidationInput
import com.example.datn.core.utils.validation.rules.lesson.ValidateLessonContentText

import java.io.InputStream

@Composable
fun AddEditLessonContentDialog(
    lessonContent: LessonContent?,
    lessonId: String,
    onDismiss: () -> Unit,
    // Callback yêu cầu mở File Picker từ bên ngoài, truyền ContentType mong muốn
    onSelectFile: (contentType: ContentType) -> Unit,
    // Trạng thái file được truyền vào từ bên ngoài (thường là ViewModel)
    selectedFileName: String?,
    selectedFileStream: InputStream?,
    selectedFileSize: Long,
    isLoading: Boolean = false,
    // Callback xác nhận thêm/sửa
    onConfirmAdd: (
        lessonId: String,
        title: String,
        description: String?,
        contentLink: String?, // Dùng cho ContentType.TEXT
        contentType: String,
        fileStream: InputStream?,
        fileSize: Long
    ) -> Unit,
    onConfirmEdit: (
        id: String,
        lessonId: String,
        title: String,
        description: String?,
        contentLink: String?, // Dùng cho ContentType.TEXT
        contentType: String,
        fileStream: InputStream?,
        fileSize: Long
    ) -> Unit
) {
    val isEditing = lessonContent != null

    var title by remember { mutableStateOf(lessonContent?.title ?: "") }

    var contentLink by remember { mutableStateOf(if (lessonContent?.contentType == ContentType.TEXT) lessonContent.content else "") }
    var selectedContentType by remember { mutableStateOf(lessonContent?.contentType ?: ContentType.TEXT) }

    // Sử dụng trạng thái file được truyền vào
    val currentFileName = selectedFileName
    val currentFileStream = selectedFileStream
    val currentFileSize = selectedFileSize

    val selectableContentTypes = remember {
        ContentType.entries.filter { it != ContentType.MINIGAME }
    }
    // State error
    var titleError by remember { mutableStateOf<String?>(null) }

    var contentError by remember { mutableStateOf<String?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val titleValidator = remember { ValidateLessonTitle() }
    val contentTextValidator = remember { ValidateLessonContentText() }
    val contentFileValidator = remember { ValidateLessonContentFile() }

    fun validateFields(): Boolean {
        val titleResult = titleValidator.validate(title)
        titleError = if (!titleResult.successful) titleResult.errorMessage else null

        contentError = when (selectedContentType) {
            ContentType.TEXT -> {
                val contentTextResult = contentTextValidator.validate(contentLink)
                if (!contentTextResult.successful) contentTextResult.errorMessage else null
            }
            else -> {
                if (isEditing) {
                    // Khi chỉnh sửa: Yêu cầu file mới chỉ khi người dùng đã chọn (currentFileStream != null),
                    // HOẶC nếu nội dung cũ không có file (lessonContent.content.isBlank()) và người dùng chưa chọn file mới.
                    if (currentFileStream != null) {
                        val fileResult = contentFileValidator.validate(
                            LessonContentFileValidationInput(
                                contentType = selectedContentType,
                                stream = currentFileStream,
                                size = currentFileSize
                            )
                        )
                        if (!fileResult.successful) fileResult.errorMessage else null
                    } else if (lessonContent?.content.isNullOrBlank()) {
                        "Nội dung cần có tệp tin. Vui lòng chọn tệp mới."
                    } else null
                } else {
                    // Khi thêm mới nội dung không phải TEXT, phải có file
                    val fileResult = contentFileValidator.validate(
                        LessonContentFileValidationInput(
                            contentType = selectedContentType,
                            stream = currentFileStream,
                            size = currentFileSize
                        )
                    )
                    if (!fileResult.successful) fileResult.errorMessage else null
                }
            }
        }

        return titleError == null && contentError == null
    }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = {
            Text(
                text = if (isEditing) "Chỉnh sửa nội dung" else "Thêm nội dung",
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
                // Tiêu đề (Giữ nguyên)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError != null) titleError = null
                    },
                    label = { Text("Tiêu đề nội dung *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                // Loại nội dung (Dropdown) (Giữ nguyên)
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded && !isEditing && !isLoading,
                    onExpandedChange = {
                        if (!isEditing && !isLoading) {
                            isDropdownExpanded = !isDropdownExpanded
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = selectedContentType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loại nội dung *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded && !isEditing && !isLoading,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        selectableContentTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedContentType = type
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Trường nhập liệu nội dung (theo loại)
                if (selectedContentType == ContentType.TEXT) {
                    // TEXT Content (Giữ nguyên)
                    OutlinedTextField(
                        value = contentLink,
                        onValueChange = {
                            contentLink = it
                            if (contentError != null) contentError = null
                        },
                        label = { Text("Nội dung văn bản * (Markdown/HTML)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        minLines = 5,
                        isError = contentError != null,
                        supportingText = { contentError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                } else {
                    // File Content (Sửa đổi logic gọi File Picker)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            // GỌI CALLBACK YÊU CẦU MỞ FILE PICKER
                            onClick = { onSelectFile(selectedContentType) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text(if (isEditing) "Chọn tệp tin mới (tùy chọn)" else "Chọn tệp tin *")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val displayFileName = when {
                            currentFileName != null -> "Đã chọn: $currentFileName"
                            isEditing && !lessonContent?.content.isNullOrBlank() -> "Tệp cũ: ${lessonContent?.content?.substringAfterLast('/')}"
                            else -> "Chưa có tệp nào"
                        }

                        Text(
                            text = displayFileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                contentError != null -> MaterialTheme.colorScheme.error
                                currentFileName != null -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        contentError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        if (isEditing) {
                            onConfirmEdit(
                                lessonContent!!.id,
                                lessonId,
                                title,
                                null,
                                contentLink.ifBlank { null },
                                selectedContentType.name,
                                // Truyền file stream và size từ trạng thái bên ngoài
                                currentFileStream,
                                currentFileSize
                            )
                        } else {
                            onConfirmAdd(
                                lessonId,
                                title,
                                null,
                                contentLink.ifBlank { null },
                                selectedContentType.name,
                                // Truyền file stream và size từ trạng thái bên ngoài
                                currentFileStream,
                                currentFileSize
                            )
                        }
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