package com.example.datn.presentation.teacher.notification

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.NotificationType
import com.example.datn.domain.usecase.notification.RecipientType
import com.example.datn.domain.usecase.notification.ReferenceObjectType

/**
 * Màn hình gửi thông báo hàng loạt
 * 
 * Giao diện cho phép:
 * - Tự động lấy ID người gửi
 * - Chọn nhóm người nhận (Giáo viên, Phụ huynh, Học sinh, Tất cả)
 * - Nhập tiêu đề và nội dung thông báo
 * - Chọn loại thông báo
 * - Thông tin tham chiếu (optional)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherNotificationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: TeacherNotificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Success Dialog
    if (state.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.onEvent(TeacherNotificationEvent.OnDismissSuccessDialog)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Thành công!",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.bulkSendResult?.let { result ->
                        Text("Tổng số người nhận: ${result.totalRecipients}")
                        Text(
                            "Gửi thành công: ${result.successCount}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (result.failedCount > 0) {
                            Text(
                                "Thất bại: ${result.failedCount}",
                                color = MaterialTheme.colorScheme.error
                            )
                            if (result.failedUsers.isNotEmpty()) {
                                Text(
                                    "Không gửi được cho: ${result.failedUsers.take(3).joinToString(", ")}${if (result.failedUsers.size > 3) "..." else ""}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } ?: Text("Thông báo đã được gửi thành công!")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.onEvent(TeacherNotificationEvent.OnDismissSuccessDialog)
                    }
                ) {
                    Text("Đóng")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Gửi Thông Báo Hàng Loạt",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Gửi Thông Báo Hàng Loạt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Gửi cho nhiều người cùng lúc",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Sender Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            text = "Người gửi",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = state.senderName ?: "Đang tải...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (state.senderId != null) {
                            Text(
                                text = "ID: ${state.senderId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Recipient Selection Section
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
                    SectionHeader(
                        icon = Icons.Default.Group,
                        title = "Người Nhận"
                    )

                    // Recipient Type Dropdown
                    var recipientExpanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = recipientExpanded,
                        onExpandedChange = { recipientExpanded = !recipientExpanded && !state.isLoading }
                    ) {
                        OutlinedTextField(
                            value = state.recipientType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Chọn nhóm người nhận") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = recipientExpanded)
                            },
                            leadingIcon = {
                                Icon(getRecipientTypeIcon(state.recipientType), contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = recipientExpanded,
                            onDismissRequest = { recipientExpanded = false }
                        ) {
                            RecipientType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = getRecipientTypeIcon(type),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(type.displayName)
                                        }
                                    },
                                    onClick = {
                                        viewModel.onEvent(TeacherNotificationEvent.OnRecipientTypeSelected(type))
                                        recipientExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Class Selection (chỉ hiển thị khi recipientType requires class)
                    if (state.recipientType.requiresClass) {
                        if (state.isLoadingClasses) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Đang tải danh sách lớp...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            var classExpanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = classExpanded,
                                onExpandedChange = { classExpanded = !classExpanded && !state.isLoading }
                            ) {
                                OutlinedTextField(
                                    value = state.availableClasses.find { it.id == state.selectedClassId }?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Chọn lớp học") },
                                    placeholder = { Text("Vui lòng chọn lớp") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Class, contentDescription = null)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    enabled = !state.isLoading && !state.isLoadingClasses,
                                    isError = state.selectedClassId.isNullOrBlank(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = classExpanded,
                                    onDismissRequest = { classExpanded = false }
                                ) {
                                    if (state.availableClasses.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Không có lớp nào") },
                                            onClick = { classExpanded = false },
                                            enabled = false
                                        )
                                    } else {
                                        state.availableClasses.forEach { classItem ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(
                                                            text = classItem.name,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            text = "Mã: ${classItem.classCode}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.onEvent(TeacherNotificationEvent.OnClassSelected(classItem.id))
                                                    classExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Hiển thị thông tin lớp đã chọn
                            state.availableClasses.find { it.id == state.selectedClassId }?.let { selectedClass ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Đã chọn: ${selectedClass.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                text = "Mã lớp: ${selectedClass.classCode}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notification Content Section
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
                    SectionHeader(
                        icon = Icons.Default.Edit,
                        title = "Nội Dung Thông Báo"
                    )

                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { 
                            viewModel.onEvent(TeacherNotificationEvent.OnTitleChanged(it))
                        },
                        label = { Text("Tiêu đề") },
                        placeholder = { Text("Nhập tiêu đề thông báo") },
                        leadingIcon = {
                            Icon(Icons.Default.Title, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isLoading
                    )

                    OutlinedTextField(
                        value = state.content,
                        onValueChange = { 
                            viewModel.onEvent(TeacherNotificationEvent.OnContentChanged(it))
                        },
                        label = { Text("Nội dung") },
                        placeholder = { Text("Nhập nội dung thông báo") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        enabled = !state.isLoading
                    )

                    // Notification Type Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded && !state.isLoading }
                    ) {
                        OutlinedTextField(
                            value = state.selectedNotificationType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Loại thông báo") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            leadingIcon = {
                                Icon(getNotificationTypeIcon(state.selectedNotificationType), contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !state.isLoading
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            NotificationType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = getNotificationTypeIcon(type),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(type.displayName)
                                        }
                                    },
                                    onClick = {
                                        viewModel.onEvent(TeacherNotificationEvent.OnNotificationTypeSelected(type))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Reference Info Section - Select from Database
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
                    SectionHeader(
                        icon = Icons.Default.Link,
                        title = "Thông Tin Tham Chiếu (Tùy chọn)"
                    )

                    // Reference Type Dropdown
                    var refTypeExpanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = refTypeExpanded,
                        onExpandedChange = { refTypeExpanded = !refTypeExpanded && !state.isLoading }
                    ) {
                        OutlinedTextField(
                            value = state.selectedReferenceType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Loại đối tượng tham chiếu") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = refTypeExpanded)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !state.isLoading
                        )

                        ExposedDropdownMenu(
                            expanded = refTypeExpanded,
                            onDismissRequest = { refTypeExpanded = false }
                        ) {
                            ReferenceObjectType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        viewModel.onEvent(TeacherNotificationEvent.OnReferenceTypeSelected(type))
                                        refTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Hiển thị dropdown chọn object nếu có available objects
                    if (state.availableReferenceObjects.isNotEmpty()) {
                        var refObjectExpanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = refObjectExpanded,
                            onExpandedChange = { refObjectExpanded = !refObjectExpanded && !state.isLoading }
                        ) {
                            OutlinedTextField(
                                value = state.selectedReferenceObject?.name ?: "Chọn ${state.selectedReferenceType.displayName.lowercase()}",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Chọn đối tượng") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = refObjectExpanded)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.List, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                enabled = !state.isLoading && !state.isLoadingReferences
                            )

                            ExposedDropdownMenu(
                                expanded = refObjectExpanded,
                                onDismissRequest = { refObjectExpanded = false }
                            ) {
                                state.availableReferenceObjects.forEach { obj ->
                                    DropdownMenuItem(
                                        text = { Text(obj.name) },
                                        onClick = {
                                            viewModel.onEvent(TeacherNotificationEvent.OnReferenceObjectSelected(obj))
                                            refObjectExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Loading indicator cho references
                    if (state.isLoadingReferences) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Đang tải danh sách...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    // Hiển thị thông tin đã chọn
                    state.selectedReferenceObject?.let { selectedObj ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Đã chọn: ${selectedObj.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "ID: ${selectedObj.id}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        viewModel.onEvent(TeacherNotificationEvent.OnResetFormClicked)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đặt lại")
                }

                Button(
                    onClick = { 
                        viewModel.onEvent(TeacherNotificationEvent.OnSendNotificationClicked)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isLoading) "Đang gửi..." else "Gửi thông báo")
                }
            }

            // Loading and Success Indicator
            AnimatedVisibility(
                visible = state.isSent && !state.isLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Thông báo đã được gửi thành công!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun getNotificationTypeIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.ASSIGNMENT -> Icons.Default.Assignment
        NotificationType.MESSAGE -> Icons.Default.Message
        NotificationType.SYSTEM_ALERT -> Icons.Default.Warning
        NotificationType.GRADE_UPDATE -> Icons.Default.Grade
        NotificationType.CLASS_UPDATE -> Icons.Default.Class
    }
}

@Composable
private fun getRecipientTypeIcon(type: RecipientType): ImageVector {
    return when (type) {
        RecipientType.SPECIFIC_USER -> Icons.Default.Person
        RecipientType.ALL_TEACHERS -> Icons.Default.School
        RecipientType.ALL_PARENTS -> Icons.Default.FamilyRestroom
        RecipientType.ALL_STUDENTS -> Icons.Default.Groups
        RecipientType.ALL_USERS -> Icons.Default.Group
        RecipientType.STUDENTS_IN_CLASS -> Icons.Default.Group
        RecipientType.PARENTS_IN_CLASS -> Icons.Default.Group
    }
}
