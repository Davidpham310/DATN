package com.example.datn.domain.usecase.user

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserByIdUseCase @Inject constructor(
    private val repository: IUserRepository
) {
    operator fun invoke(userId: String): Flow<Resource<User?>> {
        return repository.getUserById(userId)
    }
}