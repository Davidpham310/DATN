package com.example.datn.presentation.parent.relative.ui
 
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.progress.StudentDashboard
import com.example.datn.domain.usecase.progress.StudentLessonProgressItem
import com.example.datn.domain.models.StudyTimeStatistics
import com.example.datn.core.utils.extensions.formatAsDate
import com.example.datn.core.utils.extensions.formatAsDateTime
import com.example.datn.presentation.parent.relative.state.MiniGameResult
import com.example.datn.presentation.parent.relative.state.TestResult
import com.example.datn.presentation.parent.relative.viewmodel.StudentDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: String,
    studentName: String,
    onNavigateBack: () -> Unit,
    viewModel: StudentDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(studentId) {
        viewModel.loadStudentDetail(studentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(studentName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                state.studentInfo != null -> {
                    StudentDetailContent(
                        studentInfo = state.studentInfo!!,
                        dashboard = state.dashboard,
                        studyTime = state.studyTime,
                        lessonProgressItems = state.lessonProgressItems,
                        testResults = state.testResults,
                        miniGameResults = state.miniGameResults,
                        isResettingPassword = state.isResettingPassword,
                        resetPasswordSuccess = state.resetPasswordSuccess,
                        resetPasswordError = state.resetPasswordError,
                        onResetPasswordClick = { viewModel.resetStudentPassword() },
                        selectedTab = state.selectedTab,
                        onTabSelected = viewModel::onTabSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun StudentDetailContent(
    studentInfo: LinkedStudentInfo,
    dashboard: StudentDashboard?,
    studyTime: StudyTimeStatistics?,
    lessonProgressItems: List<StudentLessonProgressItem>,
    testResults: List<TestResult>,
    miniGameResults: List<MiniGameResult>,
    isResettingPassword: Boolean,
    resetPasswordSuccess: String?,
    resetPasswordError: String?,
    onResetPasswordClick: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        StudentDetailTabs(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    InfoSection(
                        title = "Thông tin cơ bản",
                        icon = Icons.Default.Person
                    ) {
                        InfoRow("Họ và tên", studentInfo.user.name)
                        InfoRow("Email", studentInfo.user.email)
                        InfoRow(
                            "Ngày sinh",
                            studentInfo.student.dateOfBirth.formatAsDate("dd/MM/yyyy")
                        )
                        InfoRow("Lớp", studentInfo.student.gradeLevel)
                        InfoRow(
                            "Mối quan hệ",
                            when (studentInfo.parentStudent.relationship.name) {
                                "FATHER" -> "Bố"
                                "MOTHER" -> "Mẹ"
                                "GRANDPARENT" -> "Ông/Bà"
                                "GUARDIAN" -> "Người giám hộ"
                                else -> studentInfo.parentStudent.relationship.name
                            }
                        )
                        if (studentInfo.parentStudent.isPrimaryGuardian) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Người giám hộ chính",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    InfoSection(
                        title = "Trạng thái tài khoản",
                        icon = Icons.Default.AccountCircle
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trạng thái",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (studentInfo.user.isActive)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = if (studentInfo.user.isActive) "Đang hoạt động" else "Không hoạt động",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (studentInfo.user.isActive)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        InfoRow(
                            "Ngày liên kết",
                            studentInfo.parentStudent.linkedAt.formatAsDateTime()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onResetPasswordClick,
                                enabled = !isResettingPassword,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isResettingPassword) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(end = 8.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text("Gửi email đổi mật khẩu cho học sinh")
                            }

                            resetPasswordSuccess?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            resetPasswordError?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (dashboard != null) {
                        InfoSection(
                            title = "Tiến độ học tập",
                            icon = Icons.Default.Timeline
                        ) {
                            val overview = dashboard.overview
                            InfoRow(
                                label = "Bài học đã hoàn thành",
                                value = "${overview.completedLessons}/${overview.totalLessons}"
                            )
                            InfoRow(
                                label = "Tiến độ trung bình",
                                value = "${overview.averageLessonProgressPercent}%"
                            )
                            InfoRow(
                                label = "Bài kiểm tra đã hoàn thành",
                                value = "${overview.completedTests}/${overview.totalTests}"
                            )
                            overview.averageTestScorePercent?.let { avg ->
                                InfoRow(
                                    label = "Điểm kiểm tra trung bình",
                                    value = String.format("%.1f/100", avg)
                                )
                            }
                        }
                    }

                    if (studyTime != null) {
                        InfoSection(
                            title = "Thời gian học",
                            icon = Icons.Default.AccessTime
                        ) {
                            InfoRow("Hôm nay", formatDuration(studyTime.todaySeconds))
                            InfoRow("Tuần này", formatDuration(studyTime.weekSeconds))
                            InfoRow("Tháng này", formatDuration(studyTime.monthSeconds))
                            InfoRow("Tổng thời gian", formatDuration(studyTime.totalSeconds))
                        }
                    }
                }

                1 -> {
                    if (lessonProgressItems.isEmpty()) {
                        EmptySection(text = "Chưa có dữ liệu tiến độ bài học")
                    } else {
                        InfoSection(
                            title = "Tiến độ từng bài học",
                            icon = Icons.Default.Book
                        ) {
                            lessonProgressItems
                                .sortedWith(compareBy({ it.className ?: "" }, { it.order }))
                                .forEach { item ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = item.lessonTitle,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val classInfo = buildString {
                                            item.className?.let { append(it) }
                                            if (!item.subject.isNullOrBlank()) {
                                                if (isNotEmpty()) append(" · ")
                                                append(item.subject)
                                            }
                                        }
                                        if (classInfo.isNotBlank()) {
                                            Text(
                                                text = classInfo,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Hoàn thành: ${item.progressPercentage}%",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (item.isCompleted) {
                                                Text(
                                                    text = "Đã hoàn thành",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Divider(modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                        }
                    }
                }

                2 -> {
                    if (testResults.isEmpty()) {
                        EmptySection(text = "Chưa có dữ liệu bài test")
                    } else {
                        testResults.forEach { result ->
                            TestResultCard(testResult = result)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                3 -> {
                    if (miniGameResults.isEmpty()) {
                        EmptySection(text = "Chưa có dữ liệu mini game")
                    } else {
                        miniGameResults.forEach { result ->
                            MiniGameResultCard(result = result)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                else -> {
                    if (studyTime == null) {
                        EmptySection(text = "Chưa có dữ liệu thời lượng học")
                    } else {
                        InfoSection(
                            title = "Thời gian học",
                            icon = Icons.Default.AccessTime
                        ) {
                            InfoRow("Hôm nay", formatDuration(studyTime.todaySeconds))
                            InfoRow("Tuần này", formatDuration(studyTime.weekSeconds))
                            InfoRow("Tháng này", formatDuration(studyTime.monthSeconds))
                            InfoRow("Tổng thời gian", formatDuration(studyTime.totalSeconds))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentDetailTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Tổng quan", "Bài học", "Bài test", "Mini game", "Thời lượng")

    TabRow(selectedTabIndex = selectedTab) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

@Composable
private fun EmptySection(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TestResultCard(testResult: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = testResult.testTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (testResult.passed) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = if (testResult.passed) "Đạt" else "Chưa đạt",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (testResult.passed) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            val scorePercent = if (testResult.maxScore > 0) {
                (testResult.score * 100f) / testResult.maxScore
            } else {
                0f
            }
            Text(
                text = "${testResult.score.toInt()}/${testResult.maxScore.toInt()} (${String.format("%.1f", scorePercent)}%)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            if (testResult.completedDate.isNotBlank()) {
                Text(
                    text = "Ngày làm: ${testResult.completedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (testResult.durationSeconds > 0L) {
                Text(
                    text = "Thời gian làm: ${formatDuration(testResult.durationSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MiniGameResultCard(result: MiniGameResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.miniGameTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "Lần ${result.attemptNumber}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Text(
                text = "${result.score.toInt()}/${result.maxScore.toInt()} (${String.format("%.1f", result.scorePercent)}%)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary
            )

            if (result.completedDate.isNotBlank()) {
                Text(
                    text = "Ngày chơi: ${result.completedDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (result.durationSeconds > 0L) {
                Text(
                    text = "Thời gian chơi: ${formatDuration(result.durationSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds <= 0L) return "0 phút 0 giây"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return when {
        hours > 0 -> "${hours} giờ ${minutes} phút ${remainingSeconds} giây"
        minutes > 0 -> "${minutes} phút ${remainingSeconds} giây"
        else -> "0 phút ${remainingSeconds} giây"
    }
}

@Composable
fun InfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Divider()
            content()
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.5f)
        )
    }
}
