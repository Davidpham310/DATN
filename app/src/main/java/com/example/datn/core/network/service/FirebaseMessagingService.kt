package com.example.datn.core.network.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseMessagingService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
        private const val PARTICIPANTS_COLLECTION = "participants"
    }

    // ==================== CONVERSATIONS ====================

    /**
     * Tạo conversation mới (1-1 hoặc group)
     */
    suspend fun createConversation(
        type: String,
        participantIds: List<String>,
        title: String? = null
    ): String {
        val conversationId = firestore.collection(CONVERSATIONS_COLLECTION).document().id
        val now = Instant.now().toEpochMilli()

        val conversationData = hashMapOf(
            "id" to conversationId,
            "type" to type,
            "title" to title,
            "lastMessageAt" to now,
            "createdAt" to now,
            "updatedAt" to now
        )

        // Tạo conversation
        firestore.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .set(conversationData)
            .await()

        // Thêm participants
        val batch = firestore.batch()
        participantIds.forEach { userId ->
            val participantRef = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(PARTICIPANTS_COLLECTION)
                .document(userId)

            val participantData = hashMapOf(
                "userId" to userId,
                "conversationId" to conversationId,
                "joinedAt" to now,
                "lastViewedAt" to if (userId == participantIds.first()) now else 0L,
                "isMuted" to false
            )

            batch.set(participantRef, participantData)
        }
        batch.commit().await()

        return conversationId
    }

    /**
     * Tìm conversation 1-1 giữa 2 users
     */
    suspend fun findOneToOneConversation(user1Id: String, user2Id: String): String? {
        val snapshot = firestore.collection(CONVERSATIONS_COLLECTION)
            .whereEqualTo("type", "ONE_TO_ONE")
            .get()
            .await()

        for (doc in snapshot.documents) {
            val conversationId = doc.id
            val participants = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(PARTICIPANTS_COLLECTION)
                .get()
                .await()

            val participantIds = participants.documents.map { it.id }
            if (participantIds.containsAll(listOf(user1Id, user2Id)) && participantIds.size == 2) {
                return conversationId
            }
        }

        return null
    }

    /**
     * Lấy conversations của user (real-time)
     */
    fun getConversationsForUser(userId: String): Flow<List<Map<String, Any?>>> = callbackFlow {
        val listener = firestore.collection(CONVERSATIONS_COLLECTION)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversations = mutableListOf<Map<String, Any?>>()
                    
                    snapshot.documents.forEach { doc ->
                        val conversationId = doc.id
                        
                        // Kiểm tra user có phải participant không
                        firestore.collection(CONVERSATIONS_COLLECTION)
                            .document(conversationId)
                            .collection(PARTICIPANTS_COLLECTION)
                            .document(userId)
                            .get()
                            .addOnSuccessListener { participantDoc ->
                                if (participantDoc.exists()) {
                                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                                    data["conversationId"] = conversationId
                                    conversations.add(data)
                                    trySend(conversations.toList())
                                }
                            }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Update lastMessageAt của conversation
     */
    suspend fun updateLastMessageAt(conversationId: String, timestamp: Long) {
        firestore.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .update(
                mapOf(
                    "lastMessageAt" to timestamp,
                    "updatedAt" to Instant.now().toEpochMilli()
                )
            )
            .await()
    }

    // ==================== MESSAGES ====================

    /**
     * Gửi message
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String,
        recipientId: String? = null
    ): String {
        val messageId = firestore.collection(MESSAGES_COLLECTION).document().id
        val now = Instant.now().toEpochMilli()

        val messageData = hashMapOf(
            "id" to messageId,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "recipientId" to recipientId,
            "content" to content,
            "sentAt" to now,
            "isRead" to false,
            "createdAt" to now,
            "updatedAt" to now
        )

        firestore.collection(MESSAGES_COLLECTION)
            .document(messageId)
            .set(messageData)
            .await()

        // Update lastMessageAt
        updateLastMessageAt(conversationId, now)

        return messageId
    }

    /**
     * Lấy messages của conversation (real-time)
     */
    fun getMessages(conversationId: String): Flow<Map<String, Any?>> = callbackFlow {
        var listener: ListenerRegistration? = null
        
        try {
            listener = firestore.collection(MESSAGES_COLLECTION)
                .whereEqualTo("conversationId", conversationId)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Check nếu là lỗi index
                        if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            // Log warning nhưng không crash
                            android.util.Log.w("FirebaseMessaging", "Index chưa sẵn sàng. Vui lòng tạo index trên Firebase Console.")
                            // Close flow nhẹ nhàng
                            close()
                        } else {
                            close(error)
                        }
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges?.forEach { change ->
                        val data = change.document.data
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED ||
                            change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED
                        ) {
                            trySend(data)
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessaging", "Error setting up listener: ${e.message}")
            close(e)
        }

        awaitClose { 
            listener?.remove() 
        }
    }

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(conversationId: String, userId: String) {
        val now = Instant.now().toEpochMilli()

        // Update participant's lastViewedAt
        firestore.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(PARTICIPANTS_COLLECTION)
            .document(userId)
            .update("lastViewedAt", now)
            .await()

        // Update messages isRead
        val messages = firestore.collection(MESSAGES_COLLECTION)
            .whereEqualTo("conversationId", conversationId)
            .whereEqualTo("isRead", false)
            .get()
            .await()

        val batch = firestore.batch()
        messages.documents.forEach { doc ->
            if (doc.getString("senderId") != userId) {
                batch.update(doc.reference, "isRead", true)
            }
        }
        batch.commit().await()
    }

    // ==================== PARTICIPANTS ====================

    /**
     * Thêm participants vào group
     */
    suspend fun addParticipants(conversationId: String, userIds: List<String>) {
        val batch = firestore.batch()
        val now = Instant.now().toEpochMilli()

        userIds.forEach { userId ->
            val participantRef = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(PARTICIPANTS_COLLECTION)
                .document(userId)

            val participantData = hashMapOf(
                "userId" to userId,
                "conversationId" to conversationId,
                "joinedAt" to now,
                "lastViewedAt" to 0L,
                "isMuted" to false
            )

            batch.set(participantRef, participantData)
        }
        batch.commit().await()
    }

    /**
     * Lấy participants của conversation
     */
    suspend fun getParticipants(conversationId: String): List<String> {
        val snapshot = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(PARTICIPANTS_COLLECTION)
            .get()
            .await()

        return snapshot.documents.map { it.id }
    }

    /**
     * Đếm unread messages
     */
    suspend fun getUnreadCount(conversationId: String, userId: String): Int {
        val participantDoc = firestore
            .collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(PARTICIPANTS_COLLECTION)
            .document(userId)
            .get()
            .await()

        val lastViewedAt = participantDoc.getLong("lastViewedAt") ?: 0L

        val unreadMessages = firestore.collection(MESSAGES_COLLECTION)
            .whereEqualTo("conversationId", conversationId)
            .whereGreaterThan("sentAt", lastViewedAt)
            .get()
            .await()

        return unreadMessages.documents.count { it.getString("senderId") != userId }
    }
}
