package com.example.datn.data.repository.impl


import com.example.datn.core.network.FirebaseAuthDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val userDao: UserDao
) : IAuthRepository {

    override fun login(email: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        val userId = firebaseAuthDataSource.login(email, password)
        val user = User(userId, email, "Unknown", UserRole.STUDENT)
        userDao.insertUser(user.toEntity()) // lưu local
        emit(Resource.Success(user))
    }.catch { e -> emit(Resource.Error(e.message ?: "Login error")) }

    override fun register(
        email: String,
        password: String,
        name: String,
        role: String
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        val userId = firebaseAuthDataSource.register(email, password)
        val user = User(userId, email, name, UserRole.valueOf(role))
        userDao.insertUser(user.toEntity())
        emit(Resource.Success(user))
    }.catch { e -> emit(Resource.Error(e.message ?: "Register error")) }

    override fun forgotPassword(email: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        firebaseAuthDataSource.sendPasswordReset(email)
        emit(Resource.Success("Password reset email sent"))
    }.catch { e -> emit(Resource.Error(e.message ?: "Forgot password error")) }
}
