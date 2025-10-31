package com.example.datn.domain.usecase.messaging

import com.example.datn.domain.models.Message
import com.example.datn.domain.repository.IMessagingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: IMessagingRepository
) {
    operator fun invoke(conversationId: String): Flow<Message> {
        return repository.getMessages(conversationId)
    }
}
