package com.example.datn.data.repository.impl

import com.example.datn.data.remote.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource
) : IUserRepository {

    override fun getUserById(userId: String): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        val result = firebaseDataSource.getUserById(userId)
        emit(result)
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun getUserByEmail(email: String): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        val result = firebaseDataSource.getUserByEmail(email)
        emit(result)
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun getUsersByRole(role: String): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        val result = firebaseDataSource.getUsersByRole(role)
        emit(result)
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun getAllUsers(): Flow<Resource<List<User>>> = flow {
        emit(Resource.Loading())
        val result = firebaseDataSource.getAllUsers()
        emit(result)
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun addUser(user: User, id: String?): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        val result = firebaseDataSource.addUser(user, id)
        emit(result)
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun updateUser(id: String, user: User): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = firebaseDataSource.updateUser(id, user)
        emit(result)
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun deleteUser(userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = firebaseDataSource.deleteUser(userId)
        emit(result)
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }

    override fun updateAvatar(userId: String, avatarUrl: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = firebaseDataSource.updateAvatar(userId, avatarUrl)
        emit(result)
    }.catch { e ->
        emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
    }
}
