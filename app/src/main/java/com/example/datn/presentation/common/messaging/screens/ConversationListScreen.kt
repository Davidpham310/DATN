package com.example.datn.presentation.common.messaging.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.data.local.dao.ConversationWithListDetails
import com.example.datn.domain.models.ConversationType
import com.example.datn.presentation.common.components.EnhancedConversationItem
import com.example.datn.presentation.common.messaging.ConversationListEvent
import com.example.datn.presentation.common.messaging.ConversationListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel = hiltViewModel(),
    onConversationClick: (String, String, String) -> Unit,
    onNewMessageClick: () -> Unit = {},
    onGroupChatClick: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    // Filter conversations based on search
    val filteredConversations = if (searchQuery.isBlank()) {
        state.conversations
    } else {
        state.conversations.filter { conversation ->
            val name = when (conversation.type) {
                ConversationType.ONE_TO_ONE -> conversation.participantName ?: ""
                ConversationType.GROUP -> getGroupDisplayName(conversation)
            }
            name.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tin nhắn")
                        if (state.totalUnreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge {
                                Text("${state.totalUnreadCount}")
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onNewMessageClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Tin nhắn mới",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onGroupChatClick) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Tạo nhóm",
                            tint = MaterialTheme.colorScheme.onPrimary
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            if (isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Tìm kiếm cuộc hội thoại...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Xóa")
                            }
                        }
                    },
                    singleLine = true
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Text(
                        text = state.error ?: "Đã xảy ra lỗi",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                filteredConversations.isEmpty() && !state.isLoading -> {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Không tìm thấy cuộc hội thoại" else "Chưa có cuộc hội thoại nào",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = filteredConversations,
                            key = { it.conversationId }
                        ) { conversation ->
                            EnhancedConversationItem(
                                conversation = conversation,
                                onClick = {
                                    // Phân biệt giữa ONE_TO_ONE và GROUP
                                    val recipientId = if (conversation.conversationType == ConversationType.ONE_TO_ONE) {
                                        conversation.participantUserId ?: ""
                                    } else {
                                        "" // Group chat không có recipientId
                                    }

                                    val displayName = if (conversation.conversationType == ConversationType.ONE_TO_ONE) {
                                        conversation.participantName ?: "Người dùng"
                                    } else {
                                        conversation.title ?: "Nhóm"
                                    }

                                    onConversationClick(
                                        conversation.conversationId,
                                        recipientId,
                                        displayName
                                    )
                                },
                                onMarkAsRead = {
                                    viewModel.onEvent(
                                        ConversationListEvent.MarkAsRead(conversation.conversationId)
                                    )
                                },
                                onMuteToggle = {
                                    viewModel.onEvent(
                                        ConversationListEvent.ToggleMute(conversation.conversationId)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

private fun getGroupDisplayName(conversation: ConversationWithListDetails): String {
    if (!conversation.title.isNullOrBlank()) {
        return conversation.title
    }
    return "Nhóm"
}
