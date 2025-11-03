package com.example.datn.domain.usecase.messaging

import com.example.datn.core.network.service.FirebaseMessagingService
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ConversationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class CreateGroupParams(
    val participantIds: List<String>,
    val groupTitle: String
)

class CreateGroupConversationUseCase @Inject constructor(
    private val firebaseMessaging: FirebaseMessagingService
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
            
            val conversationId = firebaseMessaging.createConversation(
                type = ConversationType.GROUP.name,
                participantIds = params.participantIds,
                title = params.groupTitle
            )
            
            emit(Resource.Success(conversationId))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể tạo nhóm"))
        }
    }
}
