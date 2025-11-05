package com.example.datn.domain.usecase.messaging

/**
 * Container class cho tất cả Messaging Use Cases
 * Dùng để inject vào ViewModels
 */
data class MessagingUseCases(
    // Existing use cases
    val getConversations: GetConversationsUseCase,
    val getMessages: GetMessagesUseCase,
    val sendMessage: SendMessageUseCase,
    val createConversation: CreateConversationUseCase,
    val markAsRead: MarkAsReadUseCase,
    val createGroupConversation: CreateGroupConversationUseCase,
    val addParticipants: AddParticipantsUseCase,
    val leaveGroup: LeaveGroupUseCase,
    val getGroupParticipants: GetGroupParticipantsUseCase,
    
    // New use cases for permission-based messaging
    val getAllowedRecipients: GetAllowedRecipientsUseCase,
    val checkMessagingPermission: CheckMessagingPermissionUseCase,
    val toggleMuteConversation: ToggleMuteConversationUseCase,
    val getUnreadCount: GetUnreadCountUseCase
)
