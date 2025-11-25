package com.example.datn.presentation.student.lessons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.usecase.lesson.LessonStatus
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentClassDetailScreen(
    classId: String,
    className: String,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (String, String) -> Unit,
    onWithdrawSuccess: () -> Unit = {},
    viewModel: StudentClassDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(classId) {
        viewModel.onEvent(StudentClassDetailEvent.LoadClassDetail(classId))
    }
    
    // Withdraw confirmation dialog
    if (state.showWithdrawDialog) {
        WithdrawConfirmationDialog(
            onConfirm = {
                viewModel.onEvent(StudentClassDetailEvent.ConfirmWithdraw)
                onWithdrawSuccess()
            },
            onDismiss = {
                viewModel.onEvent(StudentClassDetailEvent.DismissWithdrawDialog)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(className) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error ?: "Đã xảy ra lỗi",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                state.lessons.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có bài học nào",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Giáo viên sẽ sớm thêm bài học mới",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Class info header
                        item {
                            ClassInfoCard(
                                classInfo = state.classInfo,
                                lessonCount = state.lessons.size,
                                studentCount = state.studentCount
                            )
                        }
                        
                        // Withdraw button
                        item {
                            OutlinedButton(
                                onClick = { viewModel.onEvent(StudentClassDetailEvent.ShowWithdrawDialog) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rời khỏi lớp học")
                            }
                        }

                        // Lessons header
                        item {
                            Text(
                                text = "Danh sách bài học",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Lesson list
                        val nextUpLessonId = state.lessons
                            .firstOrNull { it.canAccess && it.status == LessonStatus.UNLOCKED && it.progress?.isCompleted != true }
                            ?.lesson?.id

                        itemsIndexed(state.lessons) { _, lessonWithStatus ->
                            val lesson = lessonWithStatus.lesson
                            val progress = lessonWithStatus.progress
                            val contentCount = state.lessonContentCounts[lesson.id] ?: 0

                            val isLocked = !lessonWithStatus.canAccess || lessonWithStatus.status == LessonStatus.LOCKED
                            val isNextUp = !isLocked && lesson.id == nextUpLessonId

                            LessonCard(
                                lesson = lesson,
                                progress = progress,
                                contentCount = contentCount,
                                isLocked = isLocked,
                                isNextUp = isNextUp,
                                onClick = {
                                    if (!isLocked) {
                                        onNavigateToLesson(lesson.id, lesson.title)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassInfoCard(
    classInfo: com.example.datn.domain.models.Class?,
    lessonCount: Int,
    studentCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            classInfo?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        icon = Icons.Default.School,
                        text = "Khối ${it.gradeLevel ?: "N/A"}"
                    )
                    InfoChip(
                        icon = Icons.Default.Book,
                        text = it.subject ?: "Chưa có môn học"
                    )
                    InfoChip(
                        icon = Icons.Default.MenuBook,
                        text = "$lessonCount bài học"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    InfoChip(
                        icon = Icons.Default.People,
                        text = "$studentCount học sinh"
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    progress: StudentLessonProgress?,
    contentCount: Int,
    isLocked: Boolean = false,
    isNextUp: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isLocked,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        val percentage = (progress?.progressPercentage ?: 0).coerceIn(0, 100)
        val isCompleted = progress?.isCompleted == true
        val viewedContents = if (contentCount > 0) {
            ((percentage / 100f) * contentCount).roundToInt().coerceIn(0, contentCount)
        } else {
            0
        }
        val timeMinutes = (progress?.timeSpentSeconds ?: 0L) / 60
        val isSeriousStudy = !isCompleted && percentage >= 80
        val statusText = when {
            isLocked -> "Hãy hoàn thành bài trước"
            progress == null || percentage == 0 -> "Chưa học"
            isCompleted -> "Đã hoàn thành"
            isSeriousStudy -> "Đang học nghiêm túc"
            else -> "Đang học"
        }
        val statusIcon = when {
            isLocked -> Icons.Default.Lock
            progress == null || percentage == 0 -> Icons.Default.RadioButtonUnchecked
            isCompleted -> Icons.Default.CheckCircle
            else -> Icons.Default.Schedule
        }
        val metaText = if (timeMinutes > 0) {
            "${percentage}% • ${timeMinutes} phút"
        } else {
            "${percentage}%"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${lesson.order}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!lesson.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lesson.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = percentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                if (contentCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đã xem: ${viewedContents}/${contentCount} nội dung",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = when {
                                progress == null || percentage == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                !isCompleted -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (isNextUp && !isLocked && !isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bài tiếp theo nên học",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Xem bài học",
                tint = if (isLocked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun WithdrawConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Rời khỏi lớp học",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Bạn có chắc chắn muốn rời khỏi lớp học này?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lưu ý:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "• Lịch sử học tập của bạn vẫn được giữ lại\n• Bạn không thể truy cập bài học mới\n• Bạn có thể xin tham gia lại sau",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
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
