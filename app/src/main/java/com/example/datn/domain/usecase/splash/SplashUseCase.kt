package com.example.datn.domain.usecase.splash

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SplashUseCase @Inject constructor(
    private val authRepository: IAuthRepository
) {
    operator fun invoke(): Flow<Resource<User?>> = authRepository.getCurrentUser()
}
