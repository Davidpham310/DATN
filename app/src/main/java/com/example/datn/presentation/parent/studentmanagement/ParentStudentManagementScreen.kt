package com.example.datn.presentation.parent.studentmanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.datn.domain.models.RelationshipType
import com.example.datn.presentation.navigation.Screen
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentStudentManagementScreen(
    navController: NavController? = null,
    viewModel: ParentStudentManagementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý học sinh") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            Row {
                FloatingActionButton(
                    onClick = { showLinkDialog = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Link, "Liên kết học sinh")
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true }
                ) {
                    Icon(Icons.Default.PersonAdd, "Tạo tài khoản học sinh")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (state.isLoading && state.linkedStudents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (state.linkedStudents.isEmpty()) {
                    EmptyStudentList()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.linkedStudents) { studentInfo ->
                            StudentCard(
                                studentInfo = studentInfo,
                                onEdit = {
                                    viewModel.onEvent(
                                        ParentStudentManagementEvent.ShowEditStudentDialog(studentInfo)
                                    )
                                },
                                onDelete = {
                                    viewModel.onEvent(
                                        ParentStudentManagementEvent.ShowDeleteConfirmDialog(studentInfo)
                                    )
                                },
                                onViewDetail = {
                                    navController?.navigate(
                                        Screen.ParentStudentDetail.createRoute(
                                            studentInfo.student.id,
                                            studentInfo.user.name
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Student Dialog
    if (showCreateDialog) {
        CreateStudentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, email, password, dateOfBirth, gradeLevel ->
                viewModel.onEvent(
                    ParentStudentManagementEvent.CreateStudentAccount(
                        name, email, password, dateOfBirth, gradeLevel
                    )
                )
                showCreateDialog = false
            }
        )
    }

    // Link Student Dialog
    if (showLinkDialog) {
        LinkStudentDialog(
            viewModel = viewModel,
            onDismiss = { showLinkDialog = false },
            onLink = { studentId, relationship, isPrimaryGuardian ->
                viewModel.onEvent(
                    ParentStudentManagementEvent.LinkStudent(
                        studentId, relationship, isPrimaryGuardian
                    )
                )
                showLinkDialog = false
            }
        )
    }

    // Edit Student Dialog
    if (state.showEditStudentDialog && state.editingStudent != null) {
        EditStudentDialog(
            studentInfo = state.editingStudent!!,
            onDismiss = {
                viewModel.onEvent(ParentStudentManagementEvent.DismissEditStudentDialog)
            },
            onUpdate = { studentId, name, dateOfBirth, gradeLevel ->
                viewModel.onEvent(
                    ParentStudentManagementEvent.UpdateStudentInfo(
                        studentId, name, dateOfBirth, gradeLevel
                    )
                )
            },
            onUpdateRelationship = { studentId, relationship, isPrimaryGuardian ->
                viewModel.onEvent(
                    ParentStudentManagementEvent.UpdateRelationship(
                        studentId, relationship, isPrimaryGuardian
                    )
                )
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (state.showDeleteConfirmDialog && state.deletingStudentInfo != null) {
        DeleteConfirmationDialog(
            studentInfo = state.deletingStudentInfo!!,
            onDismiss = {
                viewModel.onEvent(ParentStudentManagementEvent.DismissDeleteConfirmDialog)
            },
            onConfirm = {
                viewModel.onEvent(
                    ParentStudentManagementEvent.ConfirmDeleteLink(
                        state.deletingStudentInfo!!.student.id
                    )
                )
            }
        )
    }
}

@Composable
fun EmptyStudentList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Chưa có học sinh nào được liên kết",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tạo tài khoản mới hoặc liên kết với học sinh hiện có",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StudentCard(
    studentInfo: com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewDetail: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onViewDetail
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
                        text = studentInfo.user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = studentInfo.user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lớp: ${studentInfo.student.gradeLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Quan hệ: ${studentInfo.parentStudent.relationship.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (studentInfo.parentStudent.isPrimaryGuardian) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Người giám hộ chính",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Chỉnh sửa")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Xóa liên kết",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateStudentDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, LocalDate, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf(LocalDate.now()) }
    var gradeLevel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo tài khoản học sinh") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên học sinh") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = dateOfBirth.toString(),
                    onValueChange = { /* TODO: Date picker */ },
                    label = { Text("Ngày sinh (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it },
                    label = { Text("Lớp") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val dob = LocalDate.parse(dateOfBirth.toString())
                        onCreate(name, email, password, dob, gradeLevel)
                    } catch (e: Exception) {
                        // Handle error
                    }
                },
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
            ) {
                Text("Tạo")
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
fun LinkStudentDialog(
    viewModel: ParentStudentManagementViewModel,
    onDismiss: () -> Unit,
    onLink: (String, RelationshipType, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    var relationship by remember { mutableStateOf(RelationshipType.GUARDIAN) }
    var isPrimaryGuardian by remember { mutableStateOf(false) }
    val searchState by viewModel.searchState.collectAsState()

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchStudents(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Liên kết học sinh") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Tìm kiếm theo tên") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )

                // Search results
                if (searchQuery.isNotBlank()) {
                    if (searchState.isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (searchState.searchResults.isNotEmpty()) {
                        Text(
                            "Kết quả tìm kiếm:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            searchState.searchResults.forEach { result ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { selectedStudentId = result.student.id },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedStudentId == result.student.id)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                result.user.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Lớp: ${result.student.gradeLevel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (selectedStudentId == result.student.id) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "Không tìm thấy học sinh",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                if (selectedStudentId != null) {
                    Divider()
                    // Relationship selector
                    Text(
                        "Quan hệ:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    RelationshipType.values().forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = relationship == type,
                                onClick = { relationship = type }
                            )
                            Text(type.displayName)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPrimaryGuardian,
                            onCheckedChange = { isPrimaryGuardian = it }
                        )
                        Text("Người giám hộ chính")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedStudentId?.let {
                        onLink(it, relationship, isPrimaryGuardian)
                    }
                },
                enabled = selectedStudentId != null
            ) {
                Text("Liên kết")
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
fun EditStudentDialog(
    studentInfo: com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo,
    onDismiss: () -> Unit,
    onUpdate: (String, String, LocalDate, String) -> Unit,
    onUpdateRelationship: (String, RelationshipType, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(studentInfo.user.name) }
    var dateOfBirth by remember { mutableStateOf(studentInfo.student.dateOfBirth.toString()) }
    var gradeLevel by remember { mutableStateOf(studentInfo.student.gradeLevel) }
    var relationship by remember { mutableStateOf(studentInfo.parentStudent.relationship) }
    var isPrimaryGuardian by remember { mutableStateOf(studentInfo.parentStudent.isPrimaryGuardian) }
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa thông tin học sinh") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tab selector
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Thông tin") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Quan hệ") }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                when (selectedTab) {
                    0 -> {
                        // Tab thông tin học sinh
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Tên học sinh") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { dateOfBirth = it },
                            label = { Text("Ngày sinh (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = gradeLevel,
                            onValueChange = { gradeLevel = it },
                            label = { Text("Lớp") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    1 -> {
                        // Tab quan hệ
                        Text(
                            "Mối quan hệ:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        RelationshipType.values().forEach { type ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = relationship == type,
                                    onClick = { relationship = type }
                                )
                                Text(type.displayName)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isPrimaryGuardian,
                                onCheckedChange = { isPrimaryGuardian = it }
                            )
                            Text("Người giám hộ chính")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedTab == 0) {
                        try {
                            val dob = LocalDate.parse(dateOfBirth)
                            onUpdate(studentInfo.student.id, name, dob, gradeLevel)
                        } catch (e: Exception) {
                            // Handle error
                        }
                    } else {
                        onUpdateRelationship(studentInfo.student.id, relationship, isPrimaryGuardian)
                    }
                },
                enabled = name.isNotBlank() && gradeLevel.isNotBlank()
            ) {
                Text("Cập nhật")
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
fun DeleteConfirmationDialog(
    studentInfo: com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Xác nhận hủy liên kết") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bạn có chắc chắn muốn hủy liên kết với học sinh này?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Thông tin học sinh:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tên: ${studentInfo.user.name}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Email: ${studentInfo.user.email}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Lớp: ${studentInfo.student.gradeLevel}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lưu ý: Hành động này chỉ hủy liên kết giữa bạn và học sinh. Tài khoản học sinh vẫn tồn tại và có thể được liên kết lại sau.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
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

