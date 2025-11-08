package com.example.datn.presentation.teacher.classes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.Class

@Composable
fun ClassItem(
    classObj: Class,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onManageEnrollment: (() -> Unit)? = null,
    onViewMembers: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
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
                    Text(text = classObj.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Mã lớp: ${classObj.classCode}", style = MaterialTheme.typography.bodyMedium)
                }

                Row {
                    TextButton(onClick = onEdit) {
                        Text("Sửa")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onDelete) {
                        Text("Xóa")
                    }
                }
            }
            
            // Action buttons
            if (onManageEnrollment != null || onViewMembers != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View members button
                    if (onViewMembers != null) {
                        OutlinedButton(
                            onClick = onViewMembers,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xem thành viên")
                        }
                    }
                    
                    // Enrollment management button
                    if (onManageEnrollment != null) {
                        OutlinedButton(
                            onClick = onManageEnrollment,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.HowToReg,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duyệt yêu cầu")
                        }
                    }
                }
            }
        }
    }
}
