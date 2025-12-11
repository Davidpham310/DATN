package com.example.datn.domain.usecase.notification

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Notification
import com.example.datn.domain.models.NotificationType
import com.example.datn.domain.repository.INotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SendNotificationToStudentAndParentUseCase @Inject constructor(
    private val repository: INotificationRepository
) {
    operator fun invoke(params: SendNotificationParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Create notification for student
            val studentNotification = Notification(
                id = UUID.randomUUID().toString(),
                userId = params.studentId,
                senderId = params.teacherId,
                type = params.type,
                title = params.title,
                content = params.content,
                referenceObjectId = params.referenceObjectId,
                referenceObjectType = params.referenceObjectType,
                isRead = false,
                createdAt = Instant.now()
            )
            
            // Save notification for student
            repository.saveNotification(studentNotification).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        // If student has parent, also send to parent
                        params.parentId?.let { parentId ->
                            val parentNotification = Notification(
                                id = UUID.randomUUID().toString(),
                                userId = parentId,
                                senderId = params.teacherId,
                                type = params.type,
                                title = params.title,
                                content = params.content,
                                referenceObjectId = params.referenceObjectId,
                                referenceObjectType = params.referenceObjectType,
                                isRead = false,
                                createdAt = Instant.now()
                            )
                            
                            // Save notification for parent
                            repository.saveNotification(parentNotification).collect { parentResult ->
                                when (parentResult) {
                                    is Resource.Success -> emit(Resource.Success(Unit))
                                    is Resource.Error -> emit(Resource.Error(parentResult.message ?: "Failed to send notification to parent"))
                                    is Resource.Loading -> emit(Resource.Loading())
                                }
                            }
                        } ?: run {
                            emit(Resource.Success(Unit))
                        }
                    }
                    is Resource.Error -> emit(Resource.Error(result.message ?: "Failed to send notification to student"))
                    is Resource.Loading -> emit(Resource.Loading())
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
}

data class SendNotificationParams(
    val teacherId: String,
    val studentId: String,
    val parentId: String? = null,
    val type: NotificationType,
    val title: String,
    val content: String,
    val referenceObjectId: String? = null,
    val referenceObjectType: String? = null
)
