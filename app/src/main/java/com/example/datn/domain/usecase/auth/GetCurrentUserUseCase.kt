package com.example.datn.domain.usecase.auth

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    operator fun invoke(): Flow<Resource<User?>> {
        return repository.getCurrentUser()
    }
}
