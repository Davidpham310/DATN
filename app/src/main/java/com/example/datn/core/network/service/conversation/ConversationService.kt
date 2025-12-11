package com.example.datn.core.network.service.conversation

import com.example.datn.core.network.service.firestore.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Conversation
import com.example.datn.domain.models.ConversationType
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ConversationService @Inject constructor() :
    BaseFirestoreService<Conversation>(
        collectionName = "conversations",
        clazz = Conversation::class.java
    ) {

    /**
     * Lấy cuộc hội thoại theo ID
     */
    suspend fun getConversationById(conversationId: String): Conversation? {
        return getById(conversationId)
    }

    /**
     * Lấy danh sách cuộc hội thoại mà người dùng tham gia
     */
    fun getConversationsByUser(userId: String): Flow<List<Conversation>> = flow {
        val snapshot = firestore.collection("conversation_participants")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        val conversationIds = snapshot.documents.mapNotNull { it.getString("conversationId") }

        if (conversationIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        // Firestore whereIn chỉ hỗ trợ tối đa 10 items
        val conversations = conversationIds.chunked(10).flatMap { chunk ->
            collectionRef
                .whereIn("id", chunk)
                .get()
                .await()
                .documents.mapNotNull {
                    try {
                        it.internalToDomain(clazz)
                    } catch (e: Exception) {
                        null
                    }
                }
        }

        emit(conversations)
    }

    /**
     * Tìm cuộc hội thoại 1-1 giữa 2 người dùng
     */
    suspend fun findOneToOneConversation(user1Id: String, user2Id: String): Conversation? {
        // Lấy các conversation IDs của user1
        val user1Conversations = firestore.collection("conversation_participants")
            .whereEqualTo("userId", user1Id)
            .get()
            .await()
            .documents.mapNotNull { it.getString("conversationId") }

        if (user1Conversations.isEmpty()) return null

        // Tìm conversation chung
        val user2Conversations = firestore.collection("conversation_participants")
            .whereEqualTo("userId", user2Id)
            .whereIn("conversationId", user1Conversations.take(10)) // Firestore limit
            .get()
            .await()
            .documents.mapNotNull { it.getString("conversationId") }

        val sharedConversationId = user2Conversations.firstOrNull() ?: return null

        // Lấy conversation và kiểm tra type
        val conversation = getById(sharedConversationId)
        return if (conversation?.type == ConversationType.ONE_TO_ONE) conversation else null
    }

    /**
     * Tạo cuộc hội thoại mới
     */
    suspend fun createConversation(
        conversation: Conversation,
        participantIds: List<String>
    ): Conversation {
        // Tạo conversation
        val conversationId = add(null, conversation)
        
        // Thêm participants
        val batch = firestore.batch()
        participantIds.forEach { userId ->
            val participantRef = firestore.collection("conversation_participants").document()
            batch.set(participantRef, mapOf(
                "conversationId" to conversationId,
                "userId" to userId,
                "joinedAt" to FieldValue.serverTimestamp(),
                "lastViewedAt" to FieldValue.serverTimestamp(),
                "isMuted" to false
            ))
        }
        batch.commit().await()

        return conversation.copy(id = conversationId)
    }

    /**
     * Cập nhật thời gian tin nhắn cuối cùng
     */
    suspend fun updateLastMessageAt(conversationId: String) {
        collectionRef.document(conversationId)
            .update(
                mapOf(
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    /**
     * Cập nhật tiêu đề cuộc hội thoại
     */
    suspend fun updateConversationTitle(conversationId: String, title: String?) {
        collectionRef.document(conversationId)
            .update(
                mapOf(
                    "title" to title,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    /**
     * Xóa cuộc hội thoại và tất cả participants
     */
    suspend fun deleteConversationWithParticipants(conversationId: String) {
        // Xóa participants
        val participants = firestore.collection("conversation_participants")
            .whereEqualTo("conversationId", conversationId)
            .get()
            .await()

        val batch = firestore.batch()
        participants.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        
        // Xóa conversation
        batch.delete(collectionRef.document(conversationId))
        batch.commit().await()
    }
}
