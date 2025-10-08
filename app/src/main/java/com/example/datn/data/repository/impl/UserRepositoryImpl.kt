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
            val user = firebaseDataSource.getUser(userId)
            emit(Resource.Success(user))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun updateUserProfile(user: User): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Giả định FirebaseDataSource có hàm cập nhật User
            firebaseDataSource.updateUser(user)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun deleteUser(userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Giả định FirebaseDataSource có hàm xóa User
            firebaseDataSource.deleteUser(userId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }
}