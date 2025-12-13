package com.example.datn.domain.usecase.messaging

import com.example.datn.core.network.service.messaging.FirebaseMessagingService
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ConversationParticipantDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class LeaveGroupParams(
    val conversationId: String,
    val userId: String
)

class LeaveGroupUseCase @Inject constructor(
    private val firebaseMessaging: FirebaseMessagingService,
    private val participantDao: ConversationParticipantDao
) {
    operator fun invoke(params: LeaveGroupParams): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            // Xóa participant khỏi Firebase
            firebaseMessaging.removeParticipant(
                conversationId = params.conversationId,
                userId = params.userId
            )
            
            // Xóa khỏi local database
            participantDao.removeParticipant(
                conversationId = params.conversationId,
                userId = params.userId
            )
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể rời khỏi nhóm"))
        }
    }
}
