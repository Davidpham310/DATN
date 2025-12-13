package com.example.datn.presentation.student.classmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.presentation.student.classmanager.event.StudentClassEvent
import com.example.datn.presentation.student.classmanager.viewmodel.StudentClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinClassScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentClassViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var classCode by remember { mutableStateOf("") }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tham gia lớp học") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hướng dẫn",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nhập mã lớp học mà giáo viên đã cung cấp để gửi yêu cầu tham gia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Search input
            OutlinedTextField(
                value = classCode,
                onValueChange = { classCode = it.uppercase() },
                label = { Text("Mã lớp học") },
                placeholder = { Text("Ví dụ: CLASS123") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (classCode.isNotEmpty()) {
                        IconButton(onClick = { classCode = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search button
            Button(
                onClick = {
                    viewModel.onEvent(StudentClassEvent.SearchClassByCode(classCode))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = classCode.isNotBlank() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang tìm kiếm...")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tìm kiếm lớp")
                }
            }

            // Search result
            state.searchedClass?.let { classItem ->
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tìm thấy lớp học",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Class details
                        ClassDetailRow(
                            icon = Icons.Default.Class,
                            label = "Tên lớp",
                            value = classItem.name
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ClassDetailRow(
                            icon = Icons.Default.Tag,
                            label = "Mã lớp",
                            value = classItem.classCode
                        )
                        if (classItem.subject != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ClassDetailRow(
                                icon = Icons.Default.Book,
                                label = "Môn học",
                                value = classItem.subject
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ClassDetailRow(
                            icon = Icons.Default.Grade,
                            label = "Khối",
                            value = "Lớp ${classItem.gradeLevel}"
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Enrollment status or join button
                        when (state.enrollment?.enrollmentStatus) {
                            EnrollmentStatus.APPROVED -> {
                                StatusChip(
                                    text = "Đã tham gia",
                                    icon = Icons.Default.CheckCircle,
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            }
                            EnrollmentStatus.PENDING -> {
                                StatusChip(
                                    text = "Chờ phê duyệt",
                                    icon = Icons.Default.Schedule,
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            EnrollmentStatus.REJECTED -> {
                                Column {
                                    StatusChip(
                                        text = "Đã từ chối",
                                        icon = Icons.Default.Cancel,
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                    if (state.enrollment?.rejectionReason?.isNotEmpty() == true) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Lý do: ${state.enrollment?.rejectionReason}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        viewModel.onEvent(StudentClassEvent.JoinClass(classItem.id))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gửi yêu cầu tham gia")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = containerColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = containerColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = containerColor
            )
        }
    }
}
