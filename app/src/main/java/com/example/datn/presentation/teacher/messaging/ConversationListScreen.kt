package com.example.datn.presentation.teacher.messaging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.components.EnhancedConversationItem
import com.example.datn.presentation.common.messaging.ConversationListEvent
import com.example.datn.presentation.common.messaging.ConversationListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel = hiltViewModel(),
    onConversationClick: (String, String, String) -> Unit, // conversationId, recipientId, recipientName
    onNewMessageClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

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
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewMessageClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tin nhắn mới")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                state.conversations.isEmpty() -> {
                    Text(
                        text = "Chưa có cuộc hội thoại nào",
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
                            items = state.conversations,
                            key = { it.conversationId }
                        ) { conversation ->
                            EnhancedConversationItem(
                                conversation = conversation,
                                onClick = {
                                    // Debug log
                                    android.util.Log.d("TeacherConversationList", 
                                        "Conversation clicked - ID: '${conversation.conversationId}', " +
                                        "recipientId: '${conversation.participantUserId}', " +
                                        "recipientName: '${conversation.participantName}'")
                                    
                                    val recipientId = conversation.participantUserId ?: ""
                                    val recipientName = conversation.participantName ?: "Người dùng"
                                    
                                    if (recipientId.isBlank()) {
                                        android.util.Log.e("TeacherConversationList", "ERROR: recipientId is blank!")
                                    }
                                    
                                    onConversationClick(
                                        conversation.conversationId,
                                        recipientId,
                                        recipientName
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
