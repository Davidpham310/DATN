package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import kotlinx.coroutines.flow.Flow

interface ISplashRepository {
    fun getCurrentUser(): Flow<Resource<User>>
}