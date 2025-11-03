package com.example.datn.domain.usecase.messaging

data class MessagingUseCases(
    val getConversations: GetConversationsUseCase,
    val getMessages: GetMessagesUseCase,
    val sendMessage: SendMessageUseCase,
    val createConversation: CreateConversationUseCase,
    val markAsRead: MarkAsReadUseCase,
    val createGroupConversation: CreateGroupConversationUseCase,
    val addParticipants: AddParticipantsUseCase
)
