package com.example.datn.domain.repository

import com.example.datn.domain.models.User
import com.example.datn.core.utils.Resource
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    fun getUserById(userId: String): Flow<Resource<User?>>
    fun updateUserProfile(user: User): Flow<Resource<Unit>>
    fun deleteUser(userId: String): Flow<Resource<Unit>>
}