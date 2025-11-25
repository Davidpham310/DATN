package com.example.datn.presentation.common.messaging.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.ConversationType
import com.example.datn.domain.models.Message
import com.example.datn.presentation.common.messaging.ChatEvent
import com.example.datn.presentation.common.messaging.ChatViewModel
import com.example.datn.presentation.common.components.MessageBubble as CommonMessageBubble
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    recipientId: String,
    recipientName: String,
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGroupDetails: (String, String) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Load conversation when screen opens
    LaunchedEffect(conversationId) {
        viewModel.onEvent(
            ChatEvent.LoadConversation(
                conversationId = conversationId,
                recipientId = recipientId,
                recipientName = recipientName
            )
        )
    }

    // Auto scroll to bottom when new message arrives
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipientName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    // Show group info button only for group chats
                    if (conversationId != "new" && state.conversationType == ConversationType.GROUP) {
                        IconButton(
                            onClick = {
                                onNavigateToGroupDetails(conversationId, recipientName)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Thông tin nhóm",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list with background
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5)) // Light gray background như Zalo
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    state = listState
                ) {
                    items(
                        items = state.messages,
                        key = { message -> message.id }  // ← Unique key để tránh duplicate rendering
                    ) { message ->
                        ChatMessageRow(
                            message = message,
                            isCurrentUser = message.senderId == state.currentUserId,
                            isGroupChat = state.conversationType == ConversationType.GROUP,
                            senderName = state.senderNames[message.senderId]
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Input area
            MessageInputArea(
                messageInput = state.messageInput,
                isSending = state.isSending,
                onMessageChange = { viewModel.onEvent(ChatEvent.UpdateMessageInput(it)) },
                onSendClick = {
                    if (state.messageInput.isNotBlank()) {
                        viewModel.onEvent(ChatEvent.SendMessage(state.messageInput))
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: Message,
    isCurrentUser: Boolean,
    isGroupChat: Boolean,
    senderName: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser && isGroupChat) {
            // Avatar cho tin nhắn từ người khác trong group chat
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = senderName?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {
            CommonMessageBubble(
                message = message,
                isFromCurrentUser = isCurrentUser,
                showSenderName = !isCurrentUser && isGroupChat && !senderName.isNullOrBlank(),
                senderName = senderName,
                isGroupChat = isGroupChat
            )
        }
    }
}

@Composable
private fun MessageInputArea(
    messageInput: String,
    isSending: Boolean,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = messageInput,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            "Nhập tin nhắn...",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4,
                    enabled = !isSending,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Send button với style Zalo
            Surface(
                shape = CircleShape,
                color = if (messageInput.isNotBlank() && !isSending) 
                    Color(0xFF0068FF) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = onSendClick,
                    enabled = messageInput.isNotBlank() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Gửi",
                            tint = if (messageInput.isNotBlank()) 
                                Color.White 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
