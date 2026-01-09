package com.example.datn.domain.usecase.messaging

import com.example.datn.data.remote.service.messaging.FirebaseMessagingService
import com.example.datn.core.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class AddParticipantsParams(
    val conversationId: String,
    val userIds: List<String>
)

class AddParticipantsUseCase @Inject constructor(
    private val firebaseMessaging: FirebaseMessagingService
) {
    operator fun invoke(params: AddParticipantsParams): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            if (params.userIds.isEmpty()) {
                emit(Resource.Error("Danh sách thành viên trống"))
                return@flow
            }
            
            firebaseMessaging.addParticipants(
                conversationId = params.conversationId,
                userIds = params.userIds
            )
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể thêm thành viên"))
        }
    }
}
