package com.example.datn.presentation.student.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.DailyStudyTime
import com.example.datn.domain.models.StudyTimeStatistics
import com.example.datn.domain.usecase.progress.RecentActivityItem
import com.example.datn.domain.usecase.progress.RecentActivityType
import com.example.datn.domain.usecase.progress.StudentDashboardOverview
import com.example.datn.domain.usecase.progress.SubjectProgressStatistics
import com.example.datn.domain.usecase.progress.SubjectTrend
import com.example.datn.presentation.student.home.event.StudentDashboardEvent
import com.example.datn.presentation.student.home.viewmodel.StudentDashboardViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onNavigateToMyClasses: () -> Unit,
    viewModel: StudentDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val dashboard = state.dashboard

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang chủ Học sinh") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.onEvent(StudentDashboardEvent.Refresh) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Chào mừng!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Hãy bắt đầu học tập ngay hôm nay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Text(
                text = "Chức năng",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                onClick = onNavigateToMyClasses,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Class,
                            contentDescription = null,
                            modifier = Modifier.padding(14.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lớp học của tôi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Xem và quản lý các lớp đã tham gia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            when {
                state.error != null && dashboard == null && !state.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: "Đã xảy ra lỗi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(StudentDashboardEvent.Refresh) }) {
                            Text("Thử lại")
                        }
                    }
                }
                dashboard == null && state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                dashboard != null -> {
                    DashboardOverviewSection(
                        overview = dashboard.overview,
                        studyTime = state.studyTime
                    )
                    if (dashboard.subjects.isNotEmpty()) {
                        SubjectStatisticsSection(subjects = dashboard.subjects)
                    }
                    if (dashboard.recentActivities.isNotEmpty()) {
                        RecentActivitiesSection(activities = dashboard.recentActivities)
                    }
                }
            }
        }
    }
}

@Composable
fun ComingSoonCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DashboardOverviewSection(
    overview: StudentDashboardOverview,
    studyTime: StudyTimeStatistics?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tổng quan học tập",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewStatCard(
                    title = "Bài học",
                    primary = "${overview.completedLessons}/${overview.totalLessons}",
                    secondary = "${overview.averageLessonProgressPercent}% hoàn thành",
                    modifier = Modifier.weight(1f)
                )
                OverviewStatCard(
                    title = "Kiểm tra",
                    primary = "${overview.completedTests}/${overview.totalTests}",
                    secondary = overview.averageTestScorePercent?.let {
                        "${it.roundToInt()} điểm TB"
                    } ?: "Chưa có điểm",
                    modifier = Modifier.weight(1f)
                )
            }

            Divider()

            StudyTimeSection(studyTime = studyTime, fallbackTotal = overview.totalStudyTimeSeconds)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewStatCard(
                    title = "Mini game",
                    primary = overview.totalMiniGamesPlayed.toString(),
                    secondary = overview.averageMiniGameScorePercent?.let {
                        "${it.roundToInt()}% điểm TB"
                    } ?: "Chưa chơi"
                )
            }
        }
    }
}

@Composable
private fun OverviewStatCard(
    title: String,
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (secondary.isNotBlank()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StudyTimeSection(
    studyTime: StudyTimeStatistics?,
    fallbackTotal: Long
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Thời gian học",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (studyTime == null) {
            Text(
                text = "Chưa có dữ liệu thời gian học",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeStatChip(title = "Hôm nay", seconds = studyTime.todaySeconds, modifier = Modifier.weight(1f))
                    TimeStatChip(title = "Tuần này", seconds = studyTime.weekSeconds, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeStatChip(title = "Tháng này", seconds = studyTime.monthSeconds, modifier = Modifier.weight(1f))
                    TimeStatChip(title = "Tổng cộng", seconds = studyTime.totalSeconds.takeIf { it > 0 } ?: fallbackTotal, modifier = Modifier.weight(1f))
                }

                StudyTimeChart(records = studyTime.dailyRecords)
            }
        }
    }
}

@Composable
private fun TimeStatChip(
    title: String,
    seconds: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = formatDurationShort(seconds),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StudyTimeChart(
    records: List<DailyStudyTime>
) {
    if (records.isEmpty()) {
        Text(
            text = "Chưa có lịch sử thời gian học để hiển thị biểu đồ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        return
    }

    val recent = records.takeLast(7)
    val maxSeconds = recent.maxOf { it.durationSeconds }.coerceAtLeast(1L)
    val maxBarHeight = 80.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxBarHeight + 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        recent.forEach { record ->
            val ratio = (record.durationSeconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .height(maxBarHeight)
                        .width(20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(maxBarHeight * ratio)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SubjectStatisticsSection(
    subjects: List<SubjectProgressStatistics>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Thống kê theo môn",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        subjects.forEach { subject ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = subject.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Bài học: ${subject.completedLessons}/${subject.totalLessons}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Tiến độ TB: ${subject.averageLessonProgressPercent.roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    subject.averageTestScorePercent?.let {
                        Text(
                            text = "Điểm kiểm tra TB: ${"%.1f".format(it)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "Thời gian học: ${formatDurationShort(subject.totalStudyTimeSeconds)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val trendText = when (subject.trend) {
                        SubjectTrend.GOOD -> "Xu hướng: Tốt"
                        SubjectTrend.STABLE -> "Xu hướng: Ổn định"
                        SubjectTrend.NEEDS_IMPROVEMENT -> "Xu hướng: Cần cải thiện"
                    }
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (subject.trend) {
                            SubjectTrend.GOOD -> MaterialTheme.colorScheme.primary
                            SubjectTrend.STABLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            SubjectTrend.NEEDS_IMPROVEMENT -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivitiesSection(
    activities: List<RecentActivityItem>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Hoạt động gần đây",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        activities.forEach { activity ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (activity.type) {
                        RecentActivityType.LESSON -> Icons.Default.MenuBook
                        RecentActivityType.TEST -> Icons.Default.Quiz
                        RecentActivityType.MINI_GAME -> Icons.Default.Games
                    }
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activity.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        activity.subject?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        activity.scoreText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDurationShort(seconds: Long): String {
    if (seconds <= 0L) return "0 phút"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}g ${minutes}p"
        hours > 0 -> "${hours}g"
        else -> "${minutes}p"
    }
}
