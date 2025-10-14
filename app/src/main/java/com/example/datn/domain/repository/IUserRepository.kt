package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import kotlinx.coroutines.flow.Flow

interface IUserRepository {

    fun getUserById(userId: String): Flow<Resource<User?>>

    fun getUserByEmail(email: String): Flow<Resource<User?>>

    fun getUsersByRole(role: String): Flow<Resource<List<User>>>

    fun getAllUsers(): Flow<Resource<List<User>>>

    fun addUser(user: User, id: String? = null): Flow<Resource<String>>

    fun updateUser(id: String, user: User): Flow<Resource<Unit>>

    fun deleteUser(userId: String): Flow<Resource<Unit>>

    fun updateAvatar(userId: String, avatarUrl: String): Flow<Resource<Unit>>
}
