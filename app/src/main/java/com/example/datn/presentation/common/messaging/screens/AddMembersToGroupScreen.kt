package com.example.datn.presentation.common.messaging.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.User
import com.example.datn.presentation.common.messaging.AddMembersViewModel
import com.example.datn.presentation.teacher.messaging.SelectRecipientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMembersToGroupScreen(
    conversationId: String,
    onMembersAdded: () -> Unit,
    onDismiss: () -> Unit,
    // ViewModel dùng chung để select recipients (đã có sẵn)
    selectRecipientViewModel: SelectRecipientViewModel = hiltViewModel(),
    addMembersViewModel: AddMembersViewModel = hiltViewModel()
) {
    val selectState by selectRecipientViewModel.state.collectAsState()
    val addState by addMembersViewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val selectedUsers = remember { mutableStateListOf<User>() }
    var searchQuery by remember { mutableStateOf("") }
    
    val tabs = listOf(
        "Phụ huynh (${selectState.parentCount})",
        "Học sinh (${selectState.studentCount})"
    )

    // Load data when tab changes
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            selectRecipientViewModel.loadParents()
        } else {
            selectRecipientViewModel.loadStudents()
        }
    }

    // Handle success - navigate back
    LaunchedEffect(addState.isSuccess) {
        if (addState.isSuccess) {
            onMembersAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm thành viên (${selectedUsers.size})") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    if (selectedUsers.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val userIds = selectedUsers.map { it.id }
                                addMembersViewModel.addMembers(conversationId, userIds)
                            }
                        ) {
                            Icon(Icons.Default.Check, "Thêm")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    selectRecipientViewModel.search(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm kiếm...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            selectRecipientViewModel.search("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                singleLine = true
            )
            
            // Tabs for Parents/Students
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Selected users chips
            if (selectedUsers.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedUsers) { user ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedUsers.remove(user) },
                            label = { Text(user.name) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Bỏ chọn"
                                )
                            }
                        )
                    }
                }
                HorizontalDivider()
            }

            // User list or loading/error states
            when {
                selectState.isLoading || addState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                selectState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectState.error ?: "Đã xảy ra lỗi")
                    }
                }
                addState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = addState.error ?: "Đã xảy ra lỗi",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onDismiss) {
                                Text("Đóng")
                            }
                        }
                    }
                }
                selectState.filteredUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Không có người dùng")
                    }
                }
                else -> {
                    LazyColumn {
                        items(selectState.filteredUsers) { user ->
                            val isSelected = selectedUsers.any { it.id == user.id }
                            ListItem(
                                headlineContent = { Text(user.name) },
                                supportingContent = { Text(user.email) },
                                leadingContent = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedUsers.add(user)
                                            } else {
                                                selectedUsers.removeAll { it.id == user.id }
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
