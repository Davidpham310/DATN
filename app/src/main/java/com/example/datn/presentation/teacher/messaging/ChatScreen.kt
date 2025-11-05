package com.example.datn.presentation.teacher.messaging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.presentation.common.components.DateHeader
import com.example.datn.presentation.common.components.MessageBubble
import com.example.datn.presentation.common.messaging.ChatEvent
import com.example.datn.presentation.common.messaging.ChatViewModel
import com.example.datn.presentation.common.utils.groupByDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    recipientId: String,
    recipientName: String,
    viewModel: ChatViewModel = hiltViewModel(),
    onBackClick: () -> Unit
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
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
            // Messages list grouped by date
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                state = listState
            ) {
                val groupedMessages = state.messages.groupByDate()
                
                groupedMessages.forEach { (date, messages) ->
                    // Date header
                    item(key = "date_$date") {
                        DateHeader(date = date)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Messages for this date
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isFromCurrentUser = message.senderId == state.currentUserId,
                            isGroupChat = false
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
fun MessageInputArea(
    messageInput: String,
    isSending: Boolean,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Nhập tin nhắn...") },
                maxLines = 4,
                enabled = !isSending
            )
            
            IconButton(
                onClick = onSendClick,
                enabled = messageInput.isNotBlank() && !isSending,
                modifier = Modifier.size(48.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Gửi",
                        tint = if (messageInput.isNotBlank()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}
