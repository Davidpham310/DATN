package com.example.datn.domain.usecase.messaging

import com.example.datn.data.remote.datasource.FirebaseAuthDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.ConversationParticipantDao
import com.example.datn.domain.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetGroupParticipantsUseCase @Inject constructor(
    private val participantDao: ConversationParticipantDao,
    private val firebaseAuthDataSource: FirebaseAuthDataSource
) {
    operator fun invoke(conversationId: String): Flow<Resource<List<User>>> = flow {
        try {
            emit(Resource.Loading())
            
            // Get participant IDs from local database
            val participantEntities = participantDao.getParticipantsByConversation(conversationId)
            
            // Fetch user details for each participant
            val participants = mutableListOf<User>()
            participantEntities.forEach { participant ->
                try {
                    val userProfile = firebaseAuthDataSource.getUserProfile(participant.userId)
                    participants.add(userProfile)
                } catch (e: Exception) {
                    android.util.Log.e("GetGroupParticipants", "Failed to fetch user ${participant.userId}: ${e.message}")
                }
            }
            
            emit(Resource.Success(participants))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể tải danh sách thành viên"))
        }
    }
}
