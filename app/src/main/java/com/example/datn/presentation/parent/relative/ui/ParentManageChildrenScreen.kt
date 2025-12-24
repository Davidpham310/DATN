package com.example.datn.presentation.parent.relative.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.RelationshipType
import com.example.datn.presentation.parent.relative.event.ParentManageChildrenEvent
import com.example.datn.presentation.parent.relative.state.ParentManageChildrenState
import com.example.datn.presentation.parent.relative.viewmodel.ParentManageChildrenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentManageChildrenScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateStudentAccount: () -> Unit,
    viewModel: ParentManageChildrenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(ParentManageChildrenEvent.LoadLinkedStudents)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý con em") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = "Danh sách con đã liên kết",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (state.linkedStudents.isEmpty()) {
                    Text("Chưa có học sinh nào được liên kết.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.linkedStudents) { item ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.user.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Lớp: ${item.student.gradeLevel}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Quan hệ: ${item.parentStudent.relationship.displayName}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (item.parentStudent.isPrimaryGuardian) {
                                        Text(
                                            text = "(Người giám hộ chính)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = {
                                            viewModel.onEvent(
                                                ParentManageChildrenEvent.OpenRelationshipDialog(item)
                                            )
                                        }) {
                                            Text("Cập nhật quan hệ")
                                        }

                                        OutlinedButton(onClick = {
                                            viewModel.onEvent(
                                                ParentManageChildrenEvent.OpenUnlinkDialog(item)
                                            )
                                        }) {
                                            Text("Hủy liên kết")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Search existing students
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Thêm con đã có tài khoản",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = {
                        viewModel.onEvent(ParentManageChildrenEvent.UpdateSearchQuery(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm theo tên học sinh") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.onEvent(ParentManageChildrenEvent.SearchStudents)
                        }) {
                            Icon(Icons.Default.Link, contentDescription = "Tìm kiếm")
                        }
                    }
                )

                if (state.isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (state.searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.searchResults) { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(result.user.name, fontWeight = FontWeight.Bold)
                                    Text(result.user.email, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "Lớp: ${result.student.gradeLevel}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = {
                                            viewModel.onEvent(
                                                ParentManageChildrenEvent.OpenLinkDialog(result)
                                            )
                                        }) {
                                            Text("Liên kết")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (state.hasSearched && !state.isSearching) {
                    Text("Không tìm thấy học sinh phù hợp")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToCreateStudentAccount,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tạo tài khoản mới cho con")
                }
            }
        }

        if (state.showRelationshipDialog && state.selectedStudent != null) {
            RelationshipDialog(
                state = state,
                onDismiss = {
                    viewModel.onEvent(ParentManageChildrenEvent.DismissRelationshipDialog)
                },
                onRelationshipSelected = { rel ->
                    viewModel.onEvent(ParentManageChildrenEvent.ChangeRelationship(rel))
                },
                onPrimaryGuardianChanged = { isPrimary ->
                    viewModel.onEvent(ParentManageChildrenEvent.ChangePrimaryGuardian(isPrimary))
                },
                onConfirm = {
                    viewModel.onEvent(ParentManageChildrenEvent.SaveRelationship)
                }
            )
        }

        if (state.showLinkDialog && state.selectedSearchResult != null) {
            LinkStudentDialog(
                state = state,
                onDismiss = { viewModel.onEvent(ParentManageChildrenEvent.DismissLinkDialog) },
                onRelationshipSelected = { rel ->
                    viewModel.onEvent(ParentManageChildrenEvent.ChangeLinkRelationship(rel))
                },
                onPrimaryGuardianChanged = { isPrimary ->
                    viewModel.onEvent(ParentManageChildrenEvent.ChangeLinkPrimaryGuardian(isPrimary))
                },
                onConfirm = {
                    viewModel.onEvent(ParentManageChildrenEvent.ConfirmLinkStudent)
                }
            )
        }

        val selectedStudentForUnlink = state.selectedStudentForUnlink
        if (state.showUnlinkDialog && selectedStudentForUnlink != null) {
            UnlinkConfirmationDialog(
                studentName = selectedStudentForUnlink.user.name,
                onConfirm = { viewModel.onEvent(ParentManageChildrenEvent.ConfirmUnlinkStudent) },
                onDismiss = { viewModel.onEvent(ParentManageChildrenEvent.DismissUnlinkDialog) }
            )
        }
    }
}

@Composable
private fun RelationshipDialog(
    state: ParentManageChildrenState,
    onDismiss: () -> Unit,
    onRelationshipSelected: (RelationshipType) -> Unit,
    onPrimaryGuardianChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cập nhật quan hệ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RelationshipType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = state.relationshipForEdit == type,
                            onClick = { onRelationshipSelected(type) }
                        )
                        Text(text = type.displayName)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = state.isPrimaryGuardianForEdit,
                        onCheckedChange = onPrimaryGuardianChanged
                    )
                    Text("Là người giám hộ chính")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Lưu")
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
private fun UnlinkConfirmationDialog(
    studentName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xác nhận hủy liên kết") },
        text = { Text("Bạn có chắc chắn muốn hủy liên kết với học sinh $studentName?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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

@Composable
private fun LinkStudentDialog(
    state: ParentManageChildrenState,
    onDismiss: () -> Unit,
    onRelationshipSelected: (RelationshipType) -> Unit,
    onPrimaryGuardianChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit
) {
    val selected = state.selectedSearchResult

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Liên kết học sinh") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selected != null) {
                    Text(text = selected.user.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = selected.user.email, style = MaterialTheme.typography.bodySmall)
                }

                Text(text = "Chọn mối quan hệ", style = MaterialTheme.typography.titleSmall)
                RelationshipType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = state.relationshipForLink == type,
                            onClick = { onRelationshipSelected(type) }
                        )
                        Text(text = type.displayName)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = state.isPrimaryGuardianForLink,
                        onCheckedChange = onPrimaryGuardianChanged
                    )
                    Text("Là người giám hộ chính")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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
