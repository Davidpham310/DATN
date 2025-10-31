package com.example.datn.domain.usecase.messaging

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IMessagingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MarkAsReadUseCase @Inject constructor(
    private val repository: IMessagingRepository
) {
    operator fun invoke(conversationId: String, userId: String): Flow<Resource<Unit>> {
        return repository.markConversationAsRead(conversationId, userId)
    }
}
