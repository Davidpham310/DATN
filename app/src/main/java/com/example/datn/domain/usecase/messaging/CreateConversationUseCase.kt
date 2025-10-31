package com.example.datn.domain.usecase.messaging

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Conversation
import com.example.datn.domain.repository.IMessagingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateConversationUseCase @Inject constructor(
    private val repository: IMessagingRepository
) {
    operator fun invoke(user1Id: String, user2Id: String): Flow<Resource<Conversation>> {
        return repository.createOneToOneConversation(user1Id, user2Id)
    }
}
