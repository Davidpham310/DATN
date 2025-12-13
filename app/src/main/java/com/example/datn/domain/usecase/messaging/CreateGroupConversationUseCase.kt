package com.example.datn.domain.usecase.messaging

import com.example.datn.core.network.service.messaging.FirebaseMessagingService
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ConversationDao
import com.example.datn.data.local.dao.ConversationParticipantDao
import com.example.datn.data.local.entities.ConversationEntity
import com.example.datn.data.local.entities.ConversationParticipantEntity
import com.example.datn.domain.models.ConversationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject

data class CreateGroupParams(
    val participantIds: List<String>,
    val groupTitle: String
)

class CreateGroupConversationUseCase @Inject constructor(
    private val firebaseMessaging: FirebaseMessagingService,
    private val conversationDao: ConversationDao,
    private val participantDao: ConversationParticipantDao
) {
    operator fun invoke(params: CreateGroupParams): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            
            if (params.participantIds.size < 2) {
                emit(Resource.Error("Group cần ít nhất 2 thành viên"))
                return@flow
            }
            
            if (params.groupTitle.isBlank()) {
                emit(Resource.Error("Tên nhóm không được để trống"))
                return@flow
            }
            
            // 1. Tạo conversationId trước để đồng bộ giữa Room và Firebase
            val conversationId = java.util.UUID.randomUUID().toString()
            
            // 2. Tạo conversation trên Firebase với ID đã tạo
            firebaseMessaging.createConversation(
                conversationId = conversationId,  // ✅ Truyền conversationId đã tạo
                type = ConversationType.GROUP.name,
                participantIds = params.participantIds,
                title = params.groupTitle
            )
            
            // 3. Sync vào Room database ngay lập tức để hiển thị
            val now = Instant.now()
            
            // Insert conversation entity
            conversationDao.insert(
                ConversationEntity(
                    id = conversationId,
                    type = ConversationType.GROUP,
                    title = params.groupTitle,
                    lastMessageAt = now,
                    createdAt = now,
                    updatedAt = now
                )
            )
            
            // Insert participants
            params.participantIds.forEach { userId ->
                participantDao.insert(
                    ConversationParticipantEntity(
                        conversationId = conversationId,
                        userId = userId,
                        joinedAt = now,
                        lastViewedAt = now,
                        isMuted = false
                    )
                )
            }
            
            android.util.Log.d("CreateGroupUseCase", "Group conversation synced to Room: $conversationId")
            
            emit(Resource.Success(conversationId))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể tạo nhóm"))
        }
    }
}
