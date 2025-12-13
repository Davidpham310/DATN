package com.example.datn.presentation.teacher.lessons.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.ContentType
import com.example.datn.presentation.common.lesson.LessonContentManagerEvent
import com.example.datn.presentation.dialogs.ConfirmationDialog
import com.example.datn.presentation.teacher.lessons.components.AddEditLessonContentDialog
import com.example.datn.presentation.teacher.lessons.components.LessonContentList
import com.example.datn.presentation.teacher.lessons.viewmodel.LessonContentManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonContentManagerScreen(
    lessonId: String,
    lessonTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    onNavigateToMiniGame: (String, String) -> Unit = { _, _ -> },
    onNavigateToTest: (String, String) -> Unit = { _, _ -> },
    viewModel: LessonContentManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Load contents when screen opens
    LaunchedEffect(lessonId) {
        viewModel.onEvent(LessonContentManagerEvent.LoadContentsForLesson(lessonId))
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentMimeType by remember { mutableStateOf<String?>(null) }

    // Load contents when screen opens
    LaunchedEffect(lessonId) {
        viewModel.onEvent(LessonContentManagerEvent.LoadContentsForLesson(lessonId))
    }

    // --- 1. ACTIVITY RESULT LAUNCHER CHO VIỆC CHỌN FILE ---
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Lấy InputStream và kích thước file trong Coroutine để tránh chặn Main Thread
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(it)
                    val fileName = contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex("_display_name")
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: "file_upload"
                    val fileSize = contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex("_size")
                        cursor.moveToFirst()
                        cursor.getLong(sizeIndex)
                    } ?: 0L

                    if (inputStream != null && fileSize > 0) {
                        // Cập nhật trạng thái file đã chọn vào ViewModel
                        viewModel.onFileSelected(fileName, inputStream, fileSize)
                    } else {
                        Log.e("FilePicker", "Unable to open input stream or file size is zero")
                    }
                } catch (e: Exception) {
                    Log.e("FilePicker", "Error reading file URI", e)
                }
            }
        }
    }

    // Hàm mở File Picker
    val openFilePicker: (contentType: ContentType) -> Unit = { contentType ->
        currentMimeType = when (contentType) {
            ContentType.VIDEO -> "video/*"
            ContentType.PDF -> "application/pdf"
            ContentType.IMAGE -> "image/*"
            ContentType.AUDIO -> "audio/*"
            else -> "*/*" // MimeType chung cho các loại file khác
        }
        filePickerLauncher.launch(currentMimeType!!)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản lý nội dung")
                        Text(
                            text = lessonTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToMiniGame(lessonId, lessonTitle) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Games,
                            contentDescription = "Quản lý Mini Game"
                        )
                    }
                    IconButton(
                        onClick = { onNavigateToTest(lessonId, lessonTitle) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = "Quản lý Bài kiểm tra"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(LessonContentManagerEvent.ShowAddContentDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm nội dung")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.lessonContents.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    // Cần có LessonContentList composable để hiển thị danh sách
                    LessonContentList(
                        lessonContents = state.lessonContents,
                        contentUrls = state.contentUrls,
                        onEdit = { content -> viewModel.onEvent(LessonContentManagerEvent.EditContent(content)) },
                        onDelete = { content -> viewModel.onEvent(LessonContentManagerEvent.DeleteContent(content)) },
                        onClick = { content ->
                            val url = state.contentUrls[content.id] ?: content.content
                            onNavigateToDetail(content.id, url)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Loading overlay
            if (state.isLoading && state.lessonContents.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Add/Edit Content Dialog
        if (state.showAddEditDialog) {
            AddEditLessonContentDialog(
                lessonContent = state.editingContent,
                lessonId = lessonId,
                onDismiss = { viewModel.onEvent(LessonContentManagerEvent.DismissDialog) },
                onSelectFile = openFilePicker,
                // TRUYỀN CÁC TRẠNG THÁI FILE TỪ VIEWMODEL
                selectedFileName = state.selectedFileName,
                selectedFileStream = state.selectedFileStream,
                selectedFileSize = state.selectedFileSize,
                onConfirmAdd = { lessonId, title, description, contentLink, contentType, fileStream, fileSize ->
                    // description không dùng cho LessonContent, pass null
                    viewModel.onEvent(
                        LessonContentManagerEvent.ConfirmAddContent(
                            lessonId = lessonId,
                            title = title,
                            description = null,
                            contentLink = contentLink,
                            contentType = contentType,
                            fileStream = fileStream,
                            fileSize = fileSize
                        )
                    )
                },
                onConfirmEdit = { id, lessonId, title, description, contentLink, contentType, fileStream, fileSize ->
                    // description không dùng cho LessonContent, pass null
                    viewModel.onEvent(
                        LessonContentManagerEvent.ConfirmEditContent(
                            id = id,
                            lessonId = lessonId,
                            title = title,
                            description = null,
                            contentLink = contentLink,
                            contentType = contentType,
                            fileStream = fileStream, // FileStream chỉ cần cho việc upload/cập nhật file
                            fileSize = fileSize
                        )
                    )
                }
            )
        }

        // Delete Confirmation Dialog
        ConfirmationDialog(
            state = state.confirmDeleteState,
            confirmText = "Xóa",
            onDismiss = { viewModel.dismissConfirmDeleteDialog() },
            onConfirm = { content -> viewModel.confirmDeleteContent(content) }
        )
    }
}