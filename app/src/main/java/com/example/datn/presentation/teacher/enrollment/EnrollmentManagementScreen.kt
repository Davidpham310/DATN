package com.example.datn.presentation.teacher.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.datn.domain.models.EnrollmentStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentManagementScreen(
    classId: String,
    className: String,
    onNavigateBack: () -> Unit,
    viewModel: EnrollmentManagementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Load pending enrollments when screen opens
    LaunchedEffect(classId) {
        viewModel.onEvent(EnrollmentManagementEvent.LoadPendingEnrollments(classId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Yêu cầu chờ duyệt",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = className,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    // Batch approve all button
                    if (state.pendingEnrollments.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.onEvent(EnrollmentManagementEvent.BatchApproveAll(classId))
                            }
                        ) {
                            Icon(Icons.Default.DoneAll, "Duyệt tất cả")
                        }
                    }
                    
                    // Refresh button
                    IconButton(onClick = { viewModel.onEvent(EnrollmentManagementEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, "Làm mới")
                    }
                    
                    // Sort menu
                    SortMenu(
                        currentSort = state.sortBy,
                        onSortChange = { sortType ->
                            viewModel.onEvent(EnrollmentManagementEvent.UpdateSortType(sortType))
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { query ->
                    viewModel.onEvent(EnrollmentManagementEvent.UpdateSearchQuery(query))
                }
            )

            // Loading indicator
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Empty state
            else if (state.pendingEnrollments.isEmpty()) {
                EmptyState()
            }
            // Enrollment list
            else {
                EnrollmentList(
                    enrollments = filterAndSortEnrollments(
                        enrollments = state.pendingEnrollments,
                        searchQuery = state.searchQuery,
                        sortBy = state.sortBy
                    ),
                    onApprove = { enrollment ->
                        viewModel.onEvent(EnrollmentManagementEvent.SelectEnrollment(enrollment))
                        viewModel.onEvent(EnrollmentManagementEvent.ShowApproveDialog)
                    },
                    onReject = { enrollment ->
                        viewModel.onEvent(EnrollmentManagementEvent.SelectEnrollment(enrollment))
                        viewModel.onEvent(EnrollmentManagementEvent.ShowRejectDialog)
                    }
                )
            }
        }

        // Approve confirmation dialog
        state.selectedEnrollment?.let { selectedEnrollment ->
            if (state.showApproveDialog) {
                ApproveConfirmDialog(
                    studentName = selectedEnrollment.studentInfo.name,
                    onConfirm = {
                        viewModel.onEvent(
                            EnrollmentManagementEvent.ApproveEnrollment(
                                classId = classId,
                                studentId = selectedEnrollment.enrollment.studentId
                            )
                        )
                    },
                    onDismiss = {
                        viewModel.onEvent(EnrollmentManagementEvent.DismissApproveDialog)
                    }
                )
            }
        }

        // Reject dialog with reason input
        state.selectedEnrollment?.let { selectedEnrollment ->
            if (state.showRejectDialog) {
                RejectDialog(
                    studentName = selectedEnrollment.studentInfo.name,
                    rejectionReason = state.rejectionReason,
                    onReasonChange = { reason ->
                        viewModel.onEvent(EnrollmentManagementEvent.UpdateRejectionReason(reason))
                    },
                    onConfirm = {
                        viewModel.onEvent(
                            EnrollmentManagementEvent.RejectEnrollment(
                                classId = classId,
                                studentId = selectedEnrollment.enrollment.studentId,
                                reason = state.rejectionReason
                            )
                        )
                    },
                    onDismiss = {
                        viewModel.onEvent(EnrollmentManagementEvent.DismissRejectDialog)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Tìm kiếm học sinh...") },
        leadingIcon = {
            Icon(Icons.Default.Search, "Tìm kiếm")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Xóa")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SortMenu(
    currentSort: SortType,
    onSortChange: (SortType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, "Sắp xếp")
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortType.values().forEach { sortType ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(sortType.displayName)
                            if (sortType == currentSort) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        onSortChange(sortType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EnrollmentList(
    enrollments: List<EnrollmentWithStudent>,
    onApprove: (EnrollmentWithStudent) -> Unit,
    onReject: (EnrollmentWithStudent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(enrollments, key = { it.enrollment.studentId }) { enrollment ->
            EnrollmentCard(
                enrollment = enrollment,
                onApprove = { onApprove(enrollment) },
                onReject = { onReject(enrollment) }
            )
        }
    }
}

@Composable
private fun EnrollmentCard(
    enrollment: EnrollmentWithStudent,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Student info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = enrollment.studentInfo.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Student details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = enrollment.studentInfo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ID: ${enrollment.studentInfo.id.take(8)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatEnrollmentDate(enrollment.enrollment.enrolledDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status chip
                AssistChip(
                    onClick = {},
                    label = { Text("Chờ duyệt") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            // Email and phone if available
            if (enrollment.studentInfo.email.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = enrollment.studentInfo.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Divider()
            
            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reject button
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Từ chối")
                }
                
                // Approve button
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Phê duyệt")
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Không có yêu cầu chờ duyệt",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tất cả các yêu cầu đã được xử lý",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ApproveConfirmDialog(
    studentName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Xác nhận phê duyệt")
        },
        text = {
            Text("Bạn có chắc chắn muốn phê duyệt yêu cầu tham gia lớp của học sinh $studentName?")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Phê duyệt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun RejectDialog(
    studentName: String,
    rejectionReason: String,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Cancel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Từ chối yêu cầu")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Bạn đang từ chối yêu cầu tham gia lớp của học sinh $studentName.")
                
                OutlinedTextField(
                    value = rejectionReason,
                    onValueChange = onReasonChange,
                    label = { Text("Lý do từ chối *") },
                    placeholder = { Text("Nhập lý do từ chối...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = rejectionReason.isBlank()
                )
                
                if (rejectionReason.isBlank()) {
                    Text(
                        text = "Vui lòng nhập lý do từ chối",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = rejectionReason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Từ chối")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

// Helper functions
private fun filterAndSortEnrollments(
    enrollments: List<EnrollmentWithStudent>,
    searchQuery: String,
    sortBy: SortType
): List<EnrollmentWithStudent> {
    var result = enrollments
    
    // Apply search filter
    if (searchQuery.isNotBlank()) {
        result = result.filter { enrollment ->
            enrollment.studentInfo.name.contains(searchQuery, ignoreCase = true) ||
            enrollment.studentInfo.email.contains(searchQuery, ignoreCase = true)
        }
    }
    
    // Apply sorting
    result = when (sortBy) {
        SortType.BY_DATE_ASC -> result.sortedBy { it.enrollment.enrolledDate.toEpochMilli() }
        SortType.BY_DATE_DESC -> result.sortedByDescending { it.enrollment.enrolledDate.toEpochMilli() }
        SortType.BY_NAME_ASC -> result.sortedBy { it.studentInfo.name }
        SortType.BY_NAME_DESC -> result.sortedByDescending { it.studentInfo.name }
    }
    
    return result
}

private fun formatEnrollmentDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    return "Gửi lúc: ${formatter.format(instant)}"
}
