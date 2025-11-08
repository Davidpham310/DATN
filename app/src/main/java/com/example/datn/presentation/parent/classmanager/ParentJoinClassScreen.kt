package com.example.datn.presentation.parent.classmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentJoinClassScreen(
    onNavigateBack: () -> Unit,
    viewModel: ParentJoinClassViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tìm kiếm lớp học") },
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
            // Student selection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Học sinh",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (state.linkedStudents.isEmpty()) {
                        Text(
                            text = "Chưa có học sinh được liên kết",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    } else {
                        // Student selector dropdown
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = state.selectedStudent?.user?.name ?: "Chọn học sinh",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                state.linkedStudents.forEach { student ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = student.user.name,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = "Lớp ${student.student.gradeLevel}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onEvent(ParentJoinClassEvent.SelectStudent(student))
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Search type selector
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tìm kiếm theo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.searchType == SearchType.BY_CODE,
                            onClick = { 
                                viewModel.onEvent(ParentJoinClassEvent.UpdateSearchType(SearchType.BY_CODE))
                            },
                            label = { Text("Mã lớp") },
                            leadingIcon = if (state.searchType == SearchType.BY_CODE) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = state.searchType == SearchType.BY_NAME,
                            onClick = { 
                                viewModel.onEvent(ParentJoinClassEvent.UpdateSearchType(SearchType.BY_NAME))
                            },
                            label = { Text("Tên lớp") },
                            leadingIcon = if (state.searchType == SearchType.BY_NAME) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = state.searchType == SearchType.BY_SUBJECT,
                            onClick = { 
                                viewModel.onEvent(ParentJoinClassEvent.UpdateSearchType(SearchType.BY_SUBJECT))
                            },
                            label = { Text("Môn học") },
                            leadingIcon = if (state.searchType == SearchType.BY_SUBJECT) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { 
                    viewModel.onEvent(ParentJoinClassEvent.UpdateSearchQuery(it))
                },
                label = { 
                    Text(when (state.searchType) {
                        SearchType.BY_CODE -> "Nhập mã lớp"
                        SearchType.BY_NAME -> "Nhập tên lớp"
                        SearchType.BY_SUBJECT -> "Nhập môn học"
                    })
                },
                placeholder = { 
                    Text(when (state.searchType) {
                        SearchType.BY_CODE -> "Ví dụ: CLASS123"
                        SearchType.BY_NAME -> "Ví dụ: Toán 1A"
                        SearchType.BY_SUBJECT -> "Ví dụ: Toán"
                    })
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            viewModel.onEvent(ParentJoinClassEvent.ClearSearch)
                        }) {
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
                    viewModel.onEvent(ParentJoinClassEvent.SearchClasses)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.searchQuery.isNotBlank() && 
                         state.selectedStudent != null && 
                         !state.isLoading
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
                    Text("Tìm kiếm")
                }
            }

            // Search results
            if (state.searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Kết quả tìm kiếm (${state.searchResults.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.searchResults) { classItem ->
                        ClassResultCard(
                            classItem = classItem,
                            onClick = {
                                viewModel.onEvent(ParentJoinClassEvent.SelectClass(classItem))
                                viewModel.onEvent(ParentJoinClassEvent.ShowClassDetailsDialog)
                            }
                        )
                    }
                }
            }
        }
    }

    // Class details dialog
    if (state.showClassDetailsDialog && state.selectedClass != null) {
        ClassDetailsDialog(
            classItem = state.selectedClass!!,
            student = state.selectedStudent,
            enrollment = state.enrollment,
            onDismiss = {
                viewModel.onEvent(ParentJoinClassEvent.DismissClassDetailsDialog)
            },
            onJoin = { classId, studentId ->
                viewModel.onEvent(ParentJoinClassEvent.JoinClass(classId, studentId))
            }
        )
    }
}

@Composable
private fun ClassResultCard(
    classItem: com.example.datn.domain.models.Class,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = classItem.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = classItem.classCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (classItem.subject != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = classItem.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Grade,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Lớp ${classItem.gradeLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
