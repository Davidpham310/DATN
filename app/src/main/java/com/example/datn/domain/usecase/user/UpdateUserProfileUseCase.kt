package com.example.datn.domain.usecase.user

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    operator fun invoke(id: String, user: User): Flow<Resource<Unit>> {
        return userRepository.updateUser(id, user)
    }
}