package com.example.datn.presentation.parent.classmanager

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.core.utils.extensions.formatAsDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailsDialog(
    classItem: Class,
    student: LinkedStudentInfo?,
    enrollment: ClassStudent?,
    onDismiss: () -> Unit,
    onJoin: (String, String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thông tin lớp học",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Class details
                ClassDetailItem(
                    icon = Icons.Default.Class,
                    label = "Tên lớp",
                    value = classItem.name
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                ClassDetailItem(
                    icon = Icons.Default.Tag,
                    label = "Mã lớp",
                    value = classItem.classCode
                )
                
                if (classItem.subject != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ClassDetailItem(
                        icon = Icons.Default.Book,
                        label = "Môn học",
                        value = classItem.subject
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                ClassDetailItem(
                    icon = Icons.Default.Grade,
                    label = "Khối",
                    value = "Lớp ${classItem.gradeLevel}"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                ClassDetailItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Ngày tạo",
                    value = classItem.createdAt.formatAsDate()
                )

                // Student info
                if (student != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Học sinh đăng ký",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ClassDetailItem(
                        icon = Icons.Default.Person,
                        label = "Tên học sinh",
                        value = student.user.name
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ClassDetailItem(
                        icon = Icons.Default.School,
                        label = "Lớp",
                        value = "Lớp ${student.student.gradeLevel}"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Enrollment status or join button
                when (enrollment?.enrollmentStatus) {
                    EnrollmentStatus.APPROVED -> {
                        EnrollmentStatusChip(
                            text = "Đã tham gia",
                            icon = Icons.Default.CheckCircle,
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    EnrollmentStatus.PENDING -> {
                        EnrollmentStatusChip(
                            text = "Chờ phê duyệt",
                            icon = Icons.Default.Schedule,
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    EnrollmentStatus.REJECTED -> {
                        Column {
                            EnrollmentStatusChip(
                                text = "Đã từ chối",
                                icon = Icons.Default.Cancel,
                                containerColor = MaterialTheme.colorScheme.error
                            )
                            if (enrollment.rejectionReason.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Lý do: ${enrollment.rejectionReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    else -> {
                        if (student != null) {
                            Button(
                                onClick = {
                                    onJoin(classItem.id, student.student.id)
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

@Composable
private fun ClassDetailItem(
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun EnrollmentStatusChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
