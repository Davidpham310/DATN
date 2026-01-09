package com.example.datn.data.repository.impl

import com.example.datn.data.remote.service.messaging.FirebaseMessagingService
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ConversationDao
import com.example.datn.data.local.dao.ConversationParticipantDao
import com.example.datn.data.local.dao.ConversationWithListDetails
import com.example.datn.data.local.dao.MessageDao
import com.example.datn.data.local.entities.ConversationEntity
import com.example.datn.data.local.entities.ConversationParticipantEntity
import com.example.datn.data.local.entities.MessageEntity
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.Conversation
import com.example.datn.domain.models.ConversationType
import com.example.datn.domain.models.Message
import com.example.datn.domain.repository.IMessagingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MessagingRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val participantDao: ConversationParticipantDao,
    private val firebaseMessaging: FirebaseMessagingService,
    private val firebaseAuthDataSource: com.example.datn.data.remote.datasource.FirebaseAuthDataSource,
    private val userDao: com.example.datn.data.local.dao.UserDao
) : IMessagingRepository {

    override fun getConversations(userId: String): Flow<Resource<List<ConversationWithListDetails>>> = flow {
        try {
            emit(Resource.Loading())
            
            // Auto-sync: Nếu Room trống, fetch từ Firebase
            val conversationCount = conversationDao.getConversationCount(userId)
            android.util.Log.d("MessagingRepo", "▶️ getConversations called for user: $userId")
            android.util.Log.d("MessagingRepo", "▶️ Room has $conversationCount conversations")
            
            if (conversationCount == 0) {
                android.util.Log.d("MessagingRepo", "▶️ Room is empty, triggering Firebase sync...")
                syncConversationsFromFirebase(userId)
                
                // Check lại sau sync
                val newCount = conversationDao.getConversationCount(userId)
                android.util.Log.d("MessagingRepo", "▶️ After sync, Room has $newCount conversations")
            }
            
            conversationDao.getConversationsWithDetails(userId).collect { conversations ->
                android.util.Log.d("MessagingRepo", "▶️ Emitting ${conversations.size} conversations to Flow")
                // Debug unread count cho GROUP conversations
                conversations.filter { it.type == com.example.datn.domain.models.ConversationType.GROUP }
                    .forEach { conv ->
                        try {
                            val totalMessages = messageDao.countMessages(conv.conversationId)
                            val participant = participantDao.getParticipantStatus(conv.conversationId, userId)
                            val lastViewedAtMillis = participant?.lastViewedAt?.toEpochMilli() ?: 0L
                            val unreadByQuery = messageDao.countUnreadMessagesBySentAt(conv.conversationId, lastViewedAtMillis)
                            
                            android.util.Log.d("MessagingRepo", 
                                "DEBUG GROUP [${conv.title}]: " +
                                "Total messages: $totalMessages | " +
                                "LastViewedAt: ${participant?.lastViewedAt} | " +
                                "Unread (query): $unreadByQuery | " +
                                "Unread (conv): ${conv.unreadCount}"
                            )
                            
                            // List all messages with details
                            val allMessages = messageDao.getMessagesByConversation(conv.conversationId)
                            allMessages.forEach { msg ->
                                val isUnread = msg.sentAt.toEpochMilli() > lastViewedAtMillis
                                android.util.Log.d("MessagingRepo",
                                    "  Message: ${msg.id.take(8)}... | " +
                                    "SentAt: ${msg.sentAt} | " +
                                    "IsUnread: $isUnread | " +
                                    "Content: ${msg.content.take(20)}..."
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MessagingRepo", "Debug error: ${e.message}")
                        }
                    }
                
                // Auto-fetch missing user data from Firebase
                conversations.forEach { conv ->
                    // Debug: Log conversation details
                    android.util.Log.d("MessagingRepo", "Conversation ${conv.conversationId}: type=${conv.type}, participantId=${conv.participantUserId}, participantName=${conv.participantName}, title=${conv.title}, participantNames=${conv.participantNames}")
                    
                    // Nếu là ONE_TO_ONE và thiếu participantName, tự động fetch từ Firebase
                    if (conv.type == com.example.datn.domain.models.ConversationType.ONE_TO_ONE && 
                        conv.participantName == null && 
                        !conv.participantUserId.isNullOrBlank()) {  // Check cả null VÀ blank!
                        try {
                            android.util.Log.d("MessagingRepo", "Fetching user info for: ${conv.participantUserId}")
                            val userProfile = firebaseAuthDataSource.getUserProfile(conv.participantUserId!!)
                            userDao.insert(userProfile.toEntity())
                            android.util.Log.d("MessagingRepo", "Saved user: ${userProfile.name} to Room")
                        } catch (e: Exception) {
                            android.util.Log.e("MessagingRepo", "Failed to fetch user ${conv.participantUserId}: ${e.message}")
                        }
                    }
                    
                    // Nếu là GROUP và thiếu participantNames, tự động fetch all participants từ Firebase
                    if (conv.type == com.example.datn.domain.models.ConversationType.GROUP && 
                        conv.participantNames.isNullOrBlank()) {
                        try {
                            android.util.Log.d("MessagingRepo", "Fetching group participants for conversation: ${conv.conversationId}")
                            // Get all participant IDs from conversation_participant collection
                            val participants = conversationDao.getParticipantIds(conv.conversationId)
                            android.util.Log.d("MessagingRepo", "Found ${participants.size} participants")
                            
                            // Fetch each participant's user data from Firebase
                            participants.forEach { participantId ->
                                if (participantId != userId) { // Skip current user
                                    try {
                                        val userProfile = firebaseAuthDataSource.getUserProfile(participantId)
                                        userDao.insert(userProfile.toEntity())
                                        android.util.Log.d("MessagingRepo", "Saved group participant: ${userProfile.name}")
                                    } catch (e: Exception) {
                                        android.util.Log.e("MessagingRepo", "Failed to fetch participant $participantId: ${e.message}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MessagingRepo", "Failed to fetch group participants for ${conv.conversationId}: ${e.message}")
                        }
                    }
                }
                emit(Resource.Success(conversations))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể tải danh sách hội thoại"))
        }
    }

    override fun getMessages(conversationId: String): Flow<Message> = flow {
        android.util.Log.d("MessagingRepository", "⭐ getMessages listener started for conversation: $conversationId")
        
        // Track các message IDs đã emit để tránh duplicate
        val emittedMessageIds = mutableSetOf<String>()

        // Luôn sync từ Firebase TRƯỚC, sau đó mới đọc từ Room để đảm bảo có đủ history
        try {
            android.util.Log.d("MessagingRepository", "🔄 Pre-syncing messages from Firebase for: $conversationId")
            syncMessagesFromFirebase(conversationId)
        } catch (e: Exception) {
            android.util.Log.w("MessagingRepository", "⚠️ Pre-sync failed (will use cache only): ${e.message}")
        }

        // Load tất cả messages từ cache sau khi sync
        try {
            val cachedMessages = messageDao.getMessagesByConversation(conversationId)
            android.util.Log.d("MessagingRepository", "📦 Loaded ${cachedMessages.size} messages from Room for: $conversationId")

            cachedMessages.forEach { entity ->
                android.util.Log.d(
                    "MessagingRepository",
                    "  📨 Cached msg: ${entity.id.take(8)}... | From: ${entity.senderId.take(8)}... | Content: ${entity.content.take(20)}..."
                )
                emit(entity.toDomain())
                emittedMessageIds.add(entity.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepository", "❌ Failed to load cached messages: ${e.message}")
        }
        
        // Sau đó listen Firebase real-time cho messages MỚI
        try {
            android.util.Log.d("MessagingRepository", "🔥 Starting Firebase listener for: $conversationId")
            firebaseMessaging.getMessages(conversationId).collect { messageData ->
                try {
                    val messageId = messageData["id"] as? String ?: ""
                    
                    // SKIP nếu message đã được emit từ cache
                    if (emittedMessageIds.contains(messageId)) {
                        android.util.Log.d("MessagingRepository", "⏭️ Message already emitted from cache, skip: ${messageId.take(8)}...")
                        return@collect
                    }
                    
                    val message = Message(
                        id = messageId,
                        senderId = messageData["senderId"] as? String ?: "",
                        recipientId = messageData["recipientId"] as? String ?: "",
                        content = messageData["content"] as? String ?: "",
                        sentAt = Instant.ofEpochMilli(messageData["sentAt"] as? Long ?: 0L),
                        isRead = messageData["isRead"] as? Boolean ?: false,
                        conversationId = messageData["conversationId"] as? String ?: "",
                        createdAt = Instant.ofEpochMilli(messageData["createdAt"] as? Long ?: 0L),
                        updatedAt = Instant.ofEpochMilli(messageData["updatedAt"] as? Long ?: 0L)
                    )
                    
                    android.util.Log.d("MessagingRepository", "🔥 Firebase NEW message: ${message.id.take(8)}... | From: ${message.senderId.take(8)}... | Content: ${message.content.take(20)}...")
                    
                    // Check xem message đã tồn tại trong Room chưa
                    val existsInRoom = messageDao.getMessageById(messageId) != null
                    
                    if (!existsInRoom) {
                        // Lưu vào local cache chỉ khi chưa có (sử dụng IGNORE strategy)
                        messageDao.insertMessage(message.toEntity())
                        android.util.Log.d("MessagingRepository", "💾 Inserted to Room: ${messageId.take(8)}...")
                        
                        // CHỈ emit message mới (chưa có trong Room)
                        emit(message)
                        emittedMessageIds.add(messageId)
                        android.util.Log.d("MessagingRepository", "✅ New message inserted & emitted: ${message.id.take(8)}...")
                    } else {
                        android.util.Log.d("MessagingRepository", "⏭️ Message already in Room, skip insert & emit: ${messageId.take(8)}...")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MessagingRepository", "❌ Error processing Firebase message: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("MessagingRepository", "⚠️ Firebase listener failed: ${e.message}")
        }
    }

    override fun sendMessage(
        senderId: String,
        recipientId: String,
        content: String,
        conversationId: String?
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            // Nếu đã có conversationId (group chat hoặc existing conversation), dùng luôn
            var targetConversationId = conversationId
            
            // Nếu chưa có conversationId, tìm hoặc tạo conversation 1-1
            if (targetConversationId == null) {
                var conversation = conversationDao.findOneToOneConversation(senderId, recipientId)
                targetConversationId = conversation?.id
            }
            
            // Nếu vẫn chưa có (1-1 chat mới), tạo mới
            if (targetConversationId == null) {
                targetConversationId = UUID.randomUUID().toString()
                
                val newConversation = Conversation(
                    id = targetConversationId,
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
                        conversationId = targetConversationId,
                        userId = senderId,
                        joinedAt = Instant.now(),
                        lastViewedAt = Instant.now(),
                        isMuted = false
                    )
                )
                participantDao.insert(
                    ConversationParticipantEntity(
                        conversationId = targetConversationId,
                        userId = recipientId,
                        joinedAt = Instant.now(),
                        lastViewedAt = Instant.EPOCH,
                        isMuted = false
                    )
                )
                
                // Đồng bộ lên Firebase với cùng conversationId
                android.util.Log.d("MessagingRepository", "Creating conversation on Firebase: $targetConversationId")
                firebaseMessaging.createConversation(
                    conversationId = targetConversationId,  // ✅ Truyền conversationId đã tạo
                    type = ConversationType.ONE_TO_ONE.name,
                    participantIds = listOf(senderId, recipientId),
                    title = null
                )
            }

            // Check for duplicate message (cùng content, sender trong vòng 5s)
            val now = Instant.now()
            val duplicateMessage = messageDao.findDuplicateMessage(
                conversationId = targetConversationId,
                senderId = senderId,
                content = content,
                sentAtMillis = now.toEpochMilli()
            )
            
            if (duplicateMessage != null) {
                android.util.Log.w("MessagingRepository", "Duplicate message detected, skipping send: ${duplicateMessage.id}")
                emit(Resource.Success(Unit))
                return@flow
            }
            
            // TẠO messageId TRƯỚC để đồng bộ giữa Room và Firebase
            val messageId = UUID.randomUUID().toString()
            
            // Tạo message object
            val message = Message(
                id = messageId,
                senderId = senderId,
                recipientId = recipientId,
                content = content,
                sentAt = now,
                isRead = false,
                conversationId = targetConversationId,
                createdAt = now,
                updatedAt = now
            )
            
            android.util.Log.d("MessagingRepository", "📤 Uploading message to Firebase: ${message.id.take(8)}... | Content: ${content.take(20)}...")
            
            // GỬI LÊN FIREBASE TRƯỚC với messageId đã tạo
            try {
                firebaseMessaging.sendMessage(
                    messageId = messageId,  // ✅ Truyền messageId đã tạo
                    conversationId = targetConversationId,
                    senderId = senderId,
                    content = content,
                    recipientId = recipientId
                )
                
                android.util.Log.d("MessagingRepository", "✅ Firebase upload success, saving to Room...")
                
                // CHỈ lưu vào Room KHI Firebase thành công
                messageDao.insertMessage(message.toEntity())
                
                // Cập nhật lastMessageAt
                conversationDao.updateLastMessageAt(
                    targetConversationId,
                    message.sentAt.toEpochMilli(),
                    Instant.now().toEpochMilli()
                )
                
                android.util.Log.d("MessagingRepository", "💾 Message saved to Room: ${message.id.take(8)}...")
                emit(Resource.Success(Unit))
                
            } catch (e: Exception) {
                android.util.Log.e("MessagingRepository", "❌ Firebase upload failed: ${e.message}")
                emit(Resource.Error("Không thể gửi tin nhắn: ${e.message}"))
            }
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
            
            // Sử dụng timestamp hiện tại + 1 giây để chắc chắn bao gồm tất cả messages hiện tại
            val markReadTime = Instant.now().plusSeconds(1)
            
            android.util.Log.d("MessagingRepository", "Marking as read - conversationId: $conversationId, userId: $userId, time: $markReadTime")
            
            // CẬP NHẬT LOCAL CACHE TRƯỚC (ưu tiên offline-first)
            participantDao.updateLastViewed(conversationId, userId, markReadTime)
            messageDao.markMessagesAsRead(conversationId, userId)
            
            android.util.Log.d("MessagingRepository", "Local cache updated for: $conversationId")
            
            // Thử đồng bộ lên Firebase (không crash nếu fail)
            try {
                firebaseMessaging.markMessagesAsRead(conversationId, userId)
                android.util.Log.d("MessagingRepository", "Firebase sync successful for: $conversationId")
            } catch (firebaseError: Exception) {
                android.util.Log.w("MessagingRepository", "Firebase sync failed (ignoring): ${firebaseError.message}")
                // Không throw - local cache đã update thành công
            }
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepository", "Mark as read failed: ${e.message}")
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
            
            // Tạo conversationId trước để đồng bộ
            conversationId = UUID.randomUUID().toString()
            
            // Tạo conversation mới trên Firebase với ID đã tạo
            firebaseMessaging.createConversation(
                conversationId = conversationId,  // ✅ Truyền conversationId đã tạo
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
    
    private suspend fun syncConversationsFromFirebase(userId: String) {
        try {
            android.util.Log.d("MessagingRepo", "🔄 Starting Firebase sync for user: $userId")
            val conversations = firebaseMessaging.fetchConversationsForUser(userId)
            android.util.Log.d("MessagingRepo", "🔄 Firebase returned ${conversations.size} conversations")
            
            if (conversations.isEmpty()) {
                android.util.Log.w("MessagingRepo", "⚠️ No conversations found in Firebase for this user")
                return
            }
            
            conversations.forEach { data ->
                val id = data["id"] as? String ?: return@forEach
                val type = if (data["type"] == "GROUP") ConversationType.GROUP else ConversationType.ONE_TO_ONE
                
                android.util.Log.d("MessagingRepo", "🔄 Syncing conversation: $id (${type.name})")
                
                conversationDao.insert(ConversationEntity(
                    id = id,
                    type = type,
                    title = data["title"] as? String,
                    lastMessageAt = Instant.ofEpochMilli((data["lastMessageAt"] as? Long) ?: 0L),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                ))
                participantDao.insert(ConversationParticipantEntity(
                    conversationId = id,
                    userId = userId,
                    joinedAt = Instant.now(),
                    lastViewedAt = Instant.EPOCH,
                    isMuted = false
                ))
                
                // Đồng bộ luôn messages của conversation này
                android.util.Log.d("MessagingRepo", "📨 Syncing messages for conversation: $id")
                syncMessagesFromFirebase(id)
            }
            android.util.Log.d("MessagingRepo", "✅ Successfully synced ${conversations.size} conversations to Room")
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepo", "❌ Sync failed: ${e.message}", e)
        }
    }
    
    /**
     * Sync messages từ Firebase vào Room database
     */
    private suspend fun syncMessagesFromFirebase(conversationId: String) {
        try {
            android.util.Log.d("MessagingRepo", "🔄 Fetching messages from Firebase for: $conversationId")
            
            // Fetch all messages for this conversation from Firebase
            val messagesSnapshot = firebaseMessaging.firestore
                .collection("messages")
                .whereEqualTo("conversationId", conversationId)
                .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()
            
            android.util.Log.d("MessagingRepo", "🔄 Firebase returned ${messagesSnapshot.size()} messages")
            
            messagesSnapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    val messageId = data["id"] as? String ?: doc.id
                    
                    // Parse message data
                    val message = Message(
                        id = messageId,
                        senderId = data["senderId"] as? String ?: "",
                        recipientId = data["recipientId"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        sentAt = Instant.ofEpochMilli((data["sentAt"] as? Long) ?: 0L),
                        isRead = data["isRead"] as? Boolean ?: false,
                        conversationId = conversationId,
                        createdAt = Instant.ofEpochMilli((data["createdAt"] as? Long) ?: 0L),
                        updatedAt = Instant.ofEpochMilli((data["updatedAt"] as? Long) ?: 0L)
                    )
                    
                    // Insert to Room (IGNORE strategy will skip if exists)
                    messageDao.insertMessage(message.toEntity())
                    android.util.Log.d("MessagingRepo", "🔄 Synced message: ${messageId.take(8)}...")
                } catch (e: Exception) {
                    android.util.Log.e("MessagingRepo", "Error syncing message: ${e.message}")
                }
            }
            
            android.util.Log.d("MessagingRepo", "✅ Message sync completed")
        } catch (e: Exception) {
            android.util.Log.e("MessagingRepo", "❌ Message sync failed: ${e.message}", e)
        }
    }
}
