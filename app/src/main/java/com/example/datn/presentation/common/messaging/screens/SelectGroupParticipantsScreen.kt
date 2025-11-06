package com.example.datn.presentation.common.messaging.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.datn.domain.models.User
import com.example.datn.presentation.common.messaging.ConversationListViewModel

@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate", "KotlinConstantConditions")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectGroupParticipantsScreen(
    onGroupCreated: (String) -> Unit, // conversationId
    onDismiss: () -> Unit,
    selectRecipientViewModel: Any,  // Accept any ViewModel với state property
    conversationViewModel: ConversationListViewModel
) {
    // Use reflection to access methods safely
    val viewModel = selectRecipientViewModel
    val stateMethod = viewModel::class.java.getMethod("getState")
    val stateFlow = stateMethod.invoke(viewModel) as kotlinx.coroutines.flow.StateFlow<*>
    val state by stateFlow.collectAsState()
    val stateObj = state!!
    
    val conversationState by conversationViewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val selectedUsers = remember { mutableStateListOf<User>() }
    var groupName by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Tab titles with counts - use reflection
    val parentCountMethod = stateObj::class.java.getMethod("getParentCount")
    val studentCountMethod = stateObj::class.java.getMethod("getStudentCount")
    val tabs = listOf(
        "Phụ huynh (${parentCountMethod.invoke(stateObj)})",
        "Học sinh (${studentCountMethod.invoke(stateObj)})"
    )

    // Load data - use reflection
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            viewModel::class.java.getMethod("loadParents").invoke(viewModel)
        } else {
            viewModel::class.java.getMethod("loadStudents").invoke(viewModel)
        }
    }

    // Handle conversation creation success - pass both conversationId and groupTitle
    LaunchedEffect(conversationState.selectedConversationId, conversationState.createdGroupTitle) {
        if (conversationState.selectedConversationId != null && conversationState.createdGroupTitle != null) {
            // Note: onGroupCreated signature needs to be updated in NavGraph to accept groupTitle
            onGroupCreated(conversationState.selectedConversationId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo nhóm chat (${selectedUsers.size} người)") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    if (selectedUsers.size >= 2) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Check, "Tạo nhóm")
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
                    viewModel::class.java.getMethod("search", String::class.java).invoke(viewModel, it)
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
                            viewModel::class.java.getMethod("search", String::class.java).invoke(viewModel, "")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                singleLine = true
            )
            
            // Tabs
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
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Đã chọn"
                                )
                            }
                        )
                    }
                }
                HorizontalDivider()
            }

            // User list
            val isLoading = stateObj::class.java.getMethod("isLoading").invoke(stateObj) as Boolean
            val error = stateObj::class.java.getMethod("getError").invoke(stateObj) as? String
            val filteredUsers = stateObj::class.java.getMethod("getFilteredUsers").invoke(stateObj) as List<User>
            
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(error ?: "Đã xảy ra lỗi")
                    }
                }
                filteredUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Không có người dùng")
                    }
                }
                else -> {
                    LazyColumn {
                        items(filteredUsers) { user ->
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

    // Create group dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Đặt tên nhóm") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Tên nhóm") },
                    placeholder = { Text("VD: Lớp 10A, Nhóm học tập...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (groupName.isNotBlank() && selectedUsers.size >= 2) {
                            val participantIds = selectedUsers.map { it.id }
                            conversationViewModel.createGroupConversation(participantIds, groupName)
                            showCreateDialog = false
                        }
                    },
                    enabled = groupName.isNotBlank()
                ) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
