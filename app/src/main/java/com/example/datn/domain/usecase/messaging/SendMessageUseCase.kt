package com.example.datn.domain.usecase.messaging

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IMessagingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class SendMessageParams(
    val senderId: String,
    val recipientId: String,
    val content: String,
    val conversationId: String? = null // Optional: dùng khi gửi vào conversation có sẵn (group chat)
)

class SendMessageUseCase @Inject constructor(
    private val repository: IMessagingRepository
) {
    operator fun invoke(params: SendMessageParams): Flow<Resource<Unit>> {
        return repository.sendMessage(
            senderId = params.senderId,
            recipientId = params.recipientId,
            content = params.content,
            conversationId = params.conversationId
        )
    }
}
