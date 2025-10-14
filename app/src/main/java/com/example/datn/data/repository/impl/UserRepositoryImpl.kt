package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource
) : IUserRepository {

    override fun getUserById(userId: String): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getUserById(userId)
            emit(result) // ✅ Không bọc thêm Resource.Success()
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getUserByEmail(email: String): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        try {
            emit(firebaseDataSource.getUserByEmail(email))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getUsersByRole(role: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        try {
            emit(firebaseDataSource.getUsersByRole(role))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getAllUsers(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        try {
            emit(firebaseDataSource.getAllUsers())
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun addUser(user: User, id: String?): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            emit(firebaseDataSource.addUser(user, id))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun updateUser(id: String, user: User): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            emit(firebaseDataSource.updateUser(id, user))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun deleteUser(userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            emit(firebaseDataSource.deleteUser(userId))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun updateAvatar(userId: String, avatarUrl: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            emit(firebaseDataSource.updateAvatar(userId, avatarUrl))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }
}
