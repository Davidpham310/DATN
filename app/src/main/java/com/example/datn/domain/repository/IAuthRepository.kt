package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    fun login(email: String, password: String): Flow<Resource<User>>
    fun register(email: String, password: String, name: String, role: String): Flow<Resource<User>>
    fun forgotPassword(email: String): Flow<Resource<String>>
}