package com.example.datn.presentation.teacher.studentdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.usecase.progress.StudentLessonProgressItem

/**
 * Screen to display comprehensive student information
 * Including progress, test scores, and assignments
 * 
 * Can be used by both Teacher (with classId) and Parent (without classId for all classes)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: String,
    classId: String = "",
    studentName: String,
    onNavigateBack: () -> Unit,
    viewModel: StudentDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(studentId, classId) {
        viewModel.onEvent(StudentDetailEvent.LoadStudentDetail(studentId, classId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(studentName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Student info header
                item {
                    StudentInfoHeader(state = state)
                }

                // Statistics cards
                item {
                    Text(
                        text = "Tổng quan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    StatisticsCards(state = state)
                }

                item {
                    StudyAndGameSummary(state = state)
                }

                // Per-lesson progress list
                if (state.lessonProgressItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tiến độ từng bài học",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.lessonProgressItems) { lessonItem ->
                        LessonProgressRow(item = lessonItem)
                    }
                }

                // Test results
                if (state.testResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Kết quả kiểm tra",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.testResults) { testResult ->
                        TestResultCard(testResult = testResult)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonProgressRow(item: StudentLessonProgressItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.lessonTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Show class name if available (parent view)
                    if (!item.className.isNullOrEmpty()) {
                        Text(
                            text = "Lớp: ${item.className}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (item.order > 0) {
                        Text(
                            text = "Thứ tự: ${item.order}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${item.progressPercentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = (item.progressPercentage / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
private fun StudentInfoHeader(state: StudentDetailState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.userInfo?.name ?: "N/A",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.userInfo?.email ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Show grade level if available
                if (!state.userInfo?.name.isNullOrEmpty()) {
                    Text(
                        text = "Lớp: ${state.userInfo?.name?.takeLastWhile { it.isDigit() } ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (state.enrolledDate.isNotEmpty()) {
                    Text(
                        text = "Tham gia: ${state.enrolledDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsCards(state: StudentDetailState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Lesson progress
        StatCard(
            title = "Tiến độ bài học",
            value = "${state.completedLessons}/${state.totalLessons}",
            progress = state.lessonProgress,
            icon = Icons.Default.Book,
            color = MaterialTheme.colorScheme.primary
        )

        // Test scores - show if tests have been completed
        if (state.completedTests > 0) {
            StatCard(
                title = "Điểm trung bình",
                value = String.format("%.1f/100", state.averageScore),
                progress = state.averageScore / 100f,
                icon = Icons.Default.AssignmentTurnedIn,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        // Completed tests count - show if there are tests
        if (state.totalTests > 0) {
            StatCard(
                title = "Kiểm tra hoàn thành",
                value = "${state.completedTests}/${state.totalTests}",
                progress = if (state.totalTests > 0) state.completedTests.toFloat() / state.totalTests else 0f,
                icon = Icons.Default.Assignment,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun StudyAndGameSummary(state: StudentDetailState) {
    if (state.totalStudyTimeSeconds <= 0L && state.totalMiniGamesPlayed <= 0) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Hoạt động học tập",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val totalMinutes = state.totalStudyTimeSeconds / 60
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val timeText = if (hours > 0) {
                "${hours} giờ ${minutes} phút"
            } else {
                "${minutes} phút"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Thời gian học",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (state.totalMiniGamesPlayed > 0) {
                val avgMiniGameScore = state.averageMiniGameScorePercent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Mini game",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${state.totalMiniGamesPlayed} lần chơi",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${"%.1f".format(avgMiniGameScore)}% điểm TB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    progress: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = color
            )

            Text(
                text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TestResultCard(testResult: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = testResult.testTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hoàn thành: ${testResult.completedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${testResult.score.toInt()}/${testResult.maxScore.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (testResult.passed) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (testResult.passed) "Đạt" else "Không đạt",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (testResult.passed) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
