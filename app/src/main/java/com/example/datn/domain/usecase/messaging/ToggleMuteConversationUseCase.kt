package com.example.datn.domain.usecase.messaging

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IMessagingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use Case để tắt/bật thông báo cho conversation
 */
class ToggleMuteConversationUseCase @Inject constructor(
    private val messagingRepository: IMessagingRepository
) {
    operator fun invoke(conversationId: String, userId: String, isMuted: Boolean): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        
        try {
            // TODO: Implement in MessagingRepository
            // messagingRepository.updateMuteStatus(conversationId, userId, isMuted)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Không thể cập nhật trạng thái thông báo: ${e.message}"))
        }
    }
}
