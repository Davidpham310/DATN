package com.example.datn.data.repository.impl

import com.example.datn.core.network.service.FirebaseMessagingService
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ConversationDao
import com.example.datn.data.local.dao.ConversationParticipantDao
import com.example.datn.data.local.dao.ConversationWithListDetails
import com.example.datn.data.local.dao.MessageDao
import com.example.datn.data.local.entities.ConversationParticipantEntity
import com.example.datn.data.mapper.toEntity
import com.example.datn.data.mapper.toDomain
import com.example.datn.domain.models.Conversation
import com.example.datn.domain.models.ConversationType
import com.example.datn.domain.models.Message
import com.example.datn.domain.repository.IMessagingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MessagingRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val participantDao: ConversationParticipantDao,
    private val firebaseMessaging: FirebaseMessagingService
) : IMessagingRepository {

    override fun getConversations(userId: String): Flow<Resource<List<ConversationWithListDetails>>> = flow {
        try {
            emit(Resource.Loading())
            conversationDao.getConversationsWithDetails(userId).collect { conversations ->
                emit(Resource.Success(conversations))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể tải danh sách hội thoại"))
        }
    }

    override fun getMessages(conversationId: String): Flow<Message> = flow {
        try {
            // Thử lắng nghe từ Firebase real-time
            firebaseMessaging.getMessages(conversationId).collect { messageData ->
                try {
                    val message = Message(
                        id = messageData["id"] as? String ?: "",
                        senderId = messageData["senderId"] as? String ?: "",
                        recipientId = messageData["recipientId"] as? String ?: "",
                        content = messageData["content"] as? String ?: "",
                        sentAt = Instant.ofEpochMilli(messageData["sentAt"] as? Long ?: 0L),
                        isRead = messageData["isRead"] as? Boolean ?: false,
                        conversationId = messageData["conversationId"] as? String ?: "",
                        createdAt = Instant.ofEpochMilli(messageData["createdAt"] as? Long ?: 0L),
                        updatedAt = Instant.ofEpochMilli(messageData["updatedAt"] as? Long ?: 0L)
                    )
                    
                    // Lưu vào local cache
                    messageDao.insert(message.toEntity())
                    
                    // Emit message
                    emit(message)
                } catch (e: Exception) {
                    // Skip invalid messages
                }
            }
        } catch (e: Exception) {
            // Nếu Firebase fail (index chưa có), fallback về local database
            android.util.Log.w("MessagingRepository", "Firebase failed, using local cache: ${e.message}")
            
            // Load từ local cache
            messageDao.getMessagesFlow(conversationId).collect { entities ->
                entities.forEach { entity ->
                    emit(entity.toDomain())
                }
            }
        }
    }

    override fun sendMessage(
        senderId: String,
        recipientId: String,
        content: String
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            // Tìm conversation local trước
            var conversation = conversationDao.findOneToOneConversation(senderId, recipientId)
            var conversationId = conversation?.id
            
            // Nếu chưa có, tạo mới
            if (conversationId == null) {
                conversationId = UUID.randomUUID().toString()
                
                val newConversation = Conversation(
                    id = conversationId,
                    type = ConversationType.ONE_TO_ONE,
                    title = null,
                    lastMessageAt = Instant.now(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                
                // Lưu local ngay
                conversationDao.insert(newConversation.toEntity())
                
                participantDao.insert(
                    ConversationParticipantEntity(
                        conversationId = conversationId,
                        userId = senderId,
                        joinedAt = Instant.now(),
                        lastViewedAt = Instant.now(),
                        isMuted = false
                    )
                )
                participantDao.insert(
                    ConversationParticipantEntity(
                        conversationId = conversationId,
                        userId = recipientId,
                        joinedAt = Instant.now(),
                        lastViewedAt = Instant.EPOCH,
                        isMuted = false
                    )
                )
                
                // Thử sync lên Firebase (không crash nếu fail)
                try {
                    firebaseMessaging.createConversation(
                        type = ConversationType.ONE_TO_ONE.name,
                        participantIds = listOf(senderId, recipientId),
                        title = null
                    )
                } catch (e: Exception) {
                    android.util.Log.w("MessagingRepository", "Firebase sync failed: ${e.message}")
                }
            }

            // Tạo message và lưu local
            val message = Message(
                id = UUID.randomUUID().toString(),
                senderId = senderId,
                recipientId = recipientId,
                content = content,
                sentAt = Instant.now(),
                isRead = false,
                conversationId = conversationId,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            
            messageDao.insert(message.toEntity())
            
            // Cập nhật lastMessageAt
            conversationDao.updateLastMessageAt(
                conversationId,
                message.sentAt.toEpochMilli(),
                Instant.now().toEpochMilli()
            )

            // Thử gửi lên Firebase (không crash nếu fail)
            try {
                firebaseMessaging.sendMessage(
                    conversationId = conversationId,
                    senderId = senderId,
                    content = content,
                    recipientId = recipientId
                )
            } catch (e: Exception) {
                android.util.Log.w("MessagingRepository", "Firebase send failed: ${e.message}")
            }

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể gửi tin nhắn"))
        }
    }

    override fun markConversationAsRead(
        conversationId: String,
        userId: String
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            // Đánh dấu đã đọc trên Firebase
            firebaseMessaging.markMessagesAsRead(conversationId, userId)
            
            // Cập nhật local cache
            participantDao.updateLastViewed(conversationId, userId, Instant.now())
            messageDao.markMessagesAsRead(conversationId, userId)
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể đánh dấu đã đọc"))
        }
    }

    override fun createOneToOneConversation(
        user1Id: String,
        user2Id: String
    ): Flow<Resource<Conversation>> = flow {
        try {
            emit(Resource.Loading())
            
            // Tìm conversation hiện có trên Firebase
            var conversationId = firebaseMessaging.findOneToOneConversation(user1Id, user2Id)
            
            if (conversationId != null) {
                // Conversation đã tồn tại, lấy từ local
                val existing = conversationDao.getConversationById(conversationId)
                if (existing != null) {
                    emit(Resource.Success(existing.toDomain()))
                    return@flow
                }
            }
            
            // Tạo conversation mới trên Firebase
            conversationId = firebaseMessaging.createConversation(
                type = ConversationType.ONE_TO_ONE.name,
                participantIds = listOf(user1Id, user2Id),
                title = null
            )
            
            val newConversation = Conversation(
                id = conversationId,
                type = ConversationType.ONE_TO_ONE,
                title = null,
                lastMessageAt = Instant.now(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            
            // Cache vào local
            conversationDao.insert(newConversation.toEntity())
            participantDao.insert(
                ConversationParticipantEntity(
                    conversationId = conversationId,
                    userId = user1Id,
                    joinedAt = Instant.now(),
                    lastViewedAt = Instant.now(),
                    isMuted = false
                )
            )
            participantDao.insert(
                ConversationParticipantEntity(
                    conversationId = conversationId,
                    userId = user2Id,
                    joinedAt = Instant.now(),
                    lastViewedAt = Instant.EPOCH,
                    isMuted = false
                )
            )
            
            emit(Resource.Success(newConversation))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể tạo cuộc hội thoại"))
        }
    }

    override suspend fun updateLastMessageAt(
        conversationId: String,
        lastMessageAt: Instant
    ): Resource<Unit> {
        return try {
            conversationDao.updateLastMessageAt(
                conversationId,
                lastMessageAt.toEpochMilli(),
                Instant.now().toEpochMilli()
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể cập nhật thời gian tin nhắn")
        }
    }

    override suspend fun updateConversationTitle(
        conversationId: String,
        title: String?
    ): Resource<Unit> {
        return try {
            conversationDao.updateConversationTitle(
                conversationId,
                title,
                Instant.now().toEpochMilli()
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể cập nhật tiêu đề")
        }
    }

    override suspend fun deleteConversation(conversationId: String): Resource<Unit> {
        return try {
            // Xóa messages
            messageDao.deleteByConversationId(conversationId)
            // Xóa participants
            participantDao.removeAllParticipants(conversationId)
            // Xóa conversation
            conversationDao.deleteConversationById(conversationId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể xóa cuộc hội thoại")
        }
    }

    override suspend fun searchConversations(
        userId: String,
        query: String
    ): Resource<List<ConversationWithListDetails>> {
        return try {
            val result = conversationDao.searchConversations(userId, query)
            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể tìm kiếm")
        }
    }

    override suspend fun getConversationsByType(
        userId: String,
        type: ConversationType
    ): Resource<List<ConversationWithListDetails>> {
        return try {
            val result = conversationDao.getConversationsByType(userId, type.name)
            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể lọc cuộc hội thoại")
        }
    }

    override suspend fun getRecentConversations(
        userId: String,
        limit: Int
    ): Resource<List<ConversationWithListDetails>> {
        return try {
            val result = conversationDao.getRecentConversations(userId, limit)
            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể lấy cuộc hội thoại gần đây")
        }
    }

    override suspend fun getConversationsWithUnread(
        userId: String
    ): Resource<List<ConversationWithListDetails>> {
        return try {
            val result = conversationDao.getConversationsWithUnreadMessages(userId)
            Resource.Success(result)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể lấy cuộc hội thoại chưa đọc")
        }
    }

    override suspend fun getConversationCount(userId: String): Resource<Int> {
        return try {
            val count = conversationDao.getConversationCount(userId)
            Resource.Success(count)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể đếm cuộc hội thoại")
        }
    }

    override suspend fun getTotalUnreadCount(userId: String): Resource<Int> {
        return try {
            val count = conversationDao.getTotalUnreadCount(userId)
            Resource.Success(count)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể đếm tin nhắn chưa đọc")
        }
    }

    override suspend fun conversationExists(conversationId: String): Resource<Boolean> {
        return try {
            val exists = conversationDao.conversationExists(conversationId)
            Resource.Success(exists)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể kiểm tra cuộc hội thoại")
        }
    }

    override suspend fun getConversationById(conversationId: String): Resource<Conversation?> {
        return try {
            val conversation = conversationDao.getConversationById(conversationId)
            Resource.Success(conversation?.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể lấy thông tin cuộc hội thoại")
        }
    }
}
