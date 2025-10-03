package com.example.datn.domain.usecase.splash

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.ISplashRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SplashUseCase @Inject constructor(
    private val repository: ISplashRepository
){
    operator fun invoke(): Flow<Resource<User>> = repository.getCurrentUser()
}