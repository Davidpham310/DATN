package com.example.datn.domain.usecase.notification

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Notification
import com.example.datn.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSentNotificationsUseCase @Inject constructor(
    private val repository: INotificationRepository
) {
    operator fun invoke(senderId: String): Flow<Resource<List<Notification>>> {
        return repository.getNotificationsBySenderId(senderId)
    }
}
