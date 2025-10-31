package com.example.datn.core.network.service.message

import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageService @Inject constructor() :
    BaseFirestoreService<Message>(
        collectionName = "messages",
        clazz = Message::class.java
    ) {

    /**
     * Lấy tin nhắn theo ID
     */
    suspend fun getMessageById(messageId: String): Message? {
        return getById(messageId)
    }

    /**
     * Lấy danh sách tin nhắn trong cuộc hội thoại (real-time)
     */
    fun getMessages(conversationId: String): Flow<Message> = callbackFlow {
        val listener = collectionRef
            .whereEqualTo("conversationId", conversationId)
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            try {
                                val message = change.document.internalToDomain(clazz)
                                trySend(message)
                            } catch (e: Exception) {
                                // Ignore invalid messages
                            }
                        }
                        else -> { /* REMOVED - ignore */ }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Gửi tin nhắn mới
     */
    suspend fun sendMessage(message: Message): String {
        val messageData = mapOf(
            "id" to (message.id.takeIf { it.isNotEmpty() } ?: firestore.collection(collectionName).document().id),
            "senderId" to message.senderId,
            "recipientId" to message.recipientId,
            "conversationId" to message.conversationId,
            "content" to message.content,
            "isRead" to message.isRead,
            "sentAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val docRef = collectionRef.add(messageData).await()
        return docRef.id
    }

    /**
     * Đánh dấu tin nhắn là đã đọc
     */
    suspend fun markMessagesAsRead(conversationId: String, userId: String) {
        val messages = collectionRef
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        val batch = firestore.batch()
        messages.documents.forEach { doc ->
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }

    /**
     * Lấy tin nhắn cuối cùng trong cuộc hội thoại
     */
    suspend fun getLastMessage(conversationId: String): Message? {
        val snapshot = collectionRef
            .whereEqualTo("conversationId", conversationId)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.let {
            try {
                it.internalToDomain(clazz)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Đếm số tin nhắn chưa đọc trong cuộc hội thoại
     */
    suspend fun getUnreadCount(conversationId: String, userId: String): Int {
        val snapshot = collectionRef
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        return snapshot.size()
    }

    /**
     * Xóa tất cả tin nhắn trong cuộc hội thoại
     */
    suspend fun deleteMessagesByConversation(conversationId: String) {
        val messages = collectionRef
            .whereEqualTo("conversationId", conversationId)
            .get()
            .await()

        val batch = firestore.batch()
        messages.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    /**
     * Lấy danh sách tin nhắn (không real-time)
     */
    suspend fun getMessagesList(conversationId: String, limit: Int = 50): List<Message> {
        val snapshot = collectionRef
            .whereEqualTo("conversationId", conversationId)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(clazz)
            } catch (e: Exception) {
                null
            }
        }.reversed() // Reverse để có thứ tự tăng dần
    }
}
