package com.example.datn.domain.usecase.user

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UpdateAvatarUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    operator fun invoke(userId: String, avatarUrl: String): Flow<Resource<Unit>> {
        return userRepository.updateAvatar(userId, avatarUrl)
    }
}
