package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    // Authentication
    fun login(email: String, password: String , role: UserRole): Flow<Resource<User>>
    fun register(user: User, password: String): Flow<Resource<User>>
    fun forgotPassword(email : String): Flow<Resource<String>>
    fun signOut(): Flow<Resource<Unit>>
    fun getCurrentUser(): Flow<Resource<User?>>
    fun isUserLoggedIn(): Flow<Resource<Boolean>>
}