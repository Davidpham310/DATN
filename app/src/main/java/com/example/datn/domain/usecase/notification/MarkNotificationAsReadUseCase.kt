package com.example.datn.domain.usecase.notification

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MarkNotificationAsReadUseCase @Inject constructor(
    private val repository: INotificationRepository
) {
    operator fun invoke(notificationId: String): Flow<Resource<Unit>> {
        return repository.markAsRead(notificationId)
    }
}
