package com.example.datn.presentation.parent.classlist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.datn.domain.models.ClassEnrollmentInfo
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.core.utils.extensions.formatAsDate
import com.example.datn.presentation.parent.classlist.event.ParentClassListEvent
import com.example.datn.presentation.parent.classlist.viewmodel.ParentClassListViewModel

/**
 * Màn hình danh sách lớp học của phụ huynh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentClassListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToClassDetail: (String) -> Unit,
    onNavigateToJoinClass: (() -> Unit)? = null,
    viewModel: ParentClassListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    // Auto-refresh khi màn hình được mở để sync data từ Firestore
    LaunchedEffect(Unit) {
        android.util.Log.d("ParentClassListScreen", "🔄 Screen launched - Triggering refresh")
        viewModel.onEvent(ParentClassListEvent.Refresh)
    }
    
    // Log state changes
    LaunchedEffect(state.classEnrollments.size, state.isLoadingClasses, state.classesError) {
        android.util.Log.d("ParentClassListScreen", """
            📊 UI State Updated:
            - Classes: ${state.classEnrollments.size}
            - Loading: ${state.isLoadingClasses}
            - Error: ${state.classesError ?: "None"}
            - Students: ${state.linkedStudents.size}
        """.trimIndent())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lớp học của con") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    // Join Class button
                    if (onNavigateToJoinClass != null) {
                        IconButton(onClick = onNavigateToJoinClass) {
                            Icon(Icons.Default.Add, "Tìm lớp mới")
                        }
                    }
                    // Filter button
                    IconButton(onClick = { viewModel.onEvent(ParentClassListEvent.ToggleFilterDialog) }) {
                        Icon(Icons.Default.FilterList, "Lọc")
                    }
                    // Refresh button
                    IconButton(onClick = { viewModel.onEvent(ParentClassListEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, "Làm mới")
                    }
                }
            )
        },
        floatingActionButton = {
            if (onNavigateToJoinClass != null) {
                FloatingActionButton(
                    onClick = onNavigateToJoinClass,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Search, "Tìm kiếm lớp học")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            if (state.selectedStudentId != null || state.selectedEnrollmentStatus != null) {
                FilterChipsRow(
                    selectedStudent = state.linkedStudents.find { it.id == state.selectedStudentId },
                    selectedStatus = state.selectedEnrollmentStatus,
                    onClearStudent = { viewModel.onEvent(ParentClassListEvent.FilterByStudent(null)) },
                    onClearStatus = { viewModel.onEvent(ParentClassListEvent.FilterByEnrollmentStatus(null)) },
                    onClearAll = { viewModel.onEvent(ParentClassListEvent.ClearFilters) }
                )
            }

            // Content
            when {
                state.isLoadingClasses -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.classesError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.classesError ?: "Đã có lỗi xảy ra",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.onEvent(ParentClassListEvent.LoadClasses) }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
                state.classEnrollments.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chưa có lớp học nào",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.classEnrollments, key = { "${it.classId}_${it.studentId}" }) { enrollment ->
                            ClassEnrollmentCard(
                                enrollment = enrollment,
                                onClick = { onNavigateToClassDetail(enrollment.classId) }
                            )
                        }
                    }
                }
            }
        }

        // Filter Dialog
        if (state.showFilterDialog) {
            FilterDialog(
                students = state.linkedStudents,
                selectedStudentId = state.selectedStudentId,
                selectedStatus = state.selectedEnrollmentStatus,
                onDismiss = { viewModel.onEvent(ParentClassListEvent.ToggleFilterDialog) },
                onSelectStudent = { viewModel.onEvent(ParentClassListEvent.FilterByStudent(it)) },
                onSelectStatus = { viewModel.onEvent(ParentClassListEvent.FilterByEnrollmentStatus(it)) },
                onClearFilters = { viewModel.onEvent(ParentClassListEvent.ClearFilters) }
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedStudent: com.example.datn.domain.models.Student?,
    selectedStatus: EnrollmentStatus?,
    onClearStudent: () -> Unit,
    onClearStatus: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedStudent != null) {
            FilterChip(
                selected = true,
                onClick = onClearStudent,
                label = { Text(selectedStudent.id) }, // You may want to show student name instead
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa filter",
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        if (selectedStatus != null) {
            FilterChip(
                selected = true,
                onClick = onClearStatus,
                label = { Text(selectedStatus.displayName) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa filter",
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        if (selectedStudent != null || selectedStatus != null) {
            TextButton(onClick = onClearAll) {
                Text("Xóa tất cả")
            }
        }
    }
}

@Composable
private fun ClassEnrollmentCard(
    enrollment: ClassEnrollmentInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Class name and code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = enrollment.className,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mã lớp: ${enrollment.classCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EnrollmentStatusBadge(status = enrollment.enrollmentStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subject and grade
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (enrollment.subject != null) {
                    Text(
                        text = "Môn: ${enrollment.subject}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (enrollment.gradeLevel != null) {
                    Text(
                        text = "Lớp ${enrollment.gradeLevel}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Teacher info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "GV: ${enrollment.teacherName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Student info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "HS: ${enrollment.studentName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Enrollment date
            Text(
                text = "Tham gia: ${enrollment.enrolledDate.formatAsDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EnrollmentStatusBadge(
    status: EnrollmentStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        EnrollmentStatus.APPROVED -> MaterialTheme.colorScheme.primary to "Đã tham gia"
        EnrollmentStatus.PENDING -> MaterialTheme.colorScheme.tertiary to "Chờ duyệt"
        EnrollmentStatus.REJECTED -> MaterialTheme.colorScheme.error to "Từ chối"
        EnrollmentStatus.WITHDRAWN -> MaterialTheme.colorScheme.outline to "Đã hủy"
        EnrollmentStatus.NOT_ENROLLED -> MaterialTheme.colorScheme.outline to "Chưa tham gia"
    }

    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun FilterDialog(
    students: List<com.example.datn.domain.models.Student>,
    selectedStudentId: String?,
    selectedStatus: EnrollmentStatus?,
    onDismiss: () -> Unit,
    onSelectStudent: (String?) -> Unit,
    onSelectStatus: (EnrollmentStatus?) -> Unit,
    onClearFilters: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lọc lớp học") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter by student
                Text(
                    text = "Theo học sinh:",
                    style = MaterialTheme.typography.titleSmall
                )
                Column {
                    FilterChip(
                        selected = selectedStudentId == null,
                        onClick = { onSelectStudent(null) },
                        label = { Text("Tất cả") }
                    )
                    students.forEach { student ->
                        FilterChip(
                            selected = selectedStudentId == student.id,
                            onClick = { onSelectStudent(student.id) },
                            label = { Text(student.id) } // Show student name if available
                        )
                    }
                }

                Divider()

                // Filter by status
                Text(
                    text = "Theo trạng thái:",
                    style = MaterialTheme.typography.titleSmall
                )
                Column {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { onSelectStatus(null) },
                        label = { Text("Tất cả") }
                    )
                    EnrollmentStatus.values().forEach { status ->
                        if (status != EnrollmentStatus.NOT_ENROLLED) {
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { onSelectStatus(status) },
                                label = { Text(status.displayName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onClearFilters()
                onDismiss()
            }) {
                Text("Xóa filter")
            }
        }
    )
}
