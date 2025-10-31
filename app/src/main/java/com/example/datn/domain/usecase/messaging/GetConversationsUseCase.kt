package com.example.datn.domain.usecase.messaging

import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ConversationWithListDetails
import com.example.datn.domain.repository.IMessagingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val repository: IMessagingRepository
) {
    operator fun invoke(userId: String): Flow<Resource<List<ConversationWithListDetails>>> {
        return repository.getConversations(userId)
    }
}
