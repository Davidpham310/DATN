package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ConversationWithListDetails
import com.example.datn.domain.models.Conversation
import com.example.datn.domain.models.Message
import kotlinx.coroutines.flow.Flow

interface IMessagingRepository {
    fun getConversations(userId: String): Flow<Resource<List<ConversationWithListDetails>>>
    fun getMessages(conversationId: String): Flow<Message>
    fun sendMessage(senderId: String, recipientId: String, content: String): Flow<Resource<Unit>>
    fun markConversationAsRead(conversationId: String, userId: String): Flow<Resource<Unit>>
    fun createOneToOneConversation(user1Id: String, user2Id: String): Flow<Resource<Conversation>>
}