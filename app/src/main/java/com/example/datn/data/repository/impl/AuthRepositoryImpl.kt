package com.example.datn.data.repository.impl


import android.util.Log
import com.example.datn.core.network.datasource.FirebaseAuthDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.mapper.toDomain
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

    override fun login(
        email: String,
        password: String ,
        role: UserRole
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        val userId = firebaseAuthDataSource.login(email, password , role.name)
        val remoteUser = firebaseAuthDataSource.getUserProfile(userId)
        val userEntity = remoteUser.toEntity()
        if (userDao.isUserExists(remoteUser.id)) {
            userDao.update(userEntity)
            Log.d("AuthRepositoryImpl", "User updated in local database")
        } else {
            userDao.insert(userEntity)
            Log.d("AuthRepositoryImpl", "User added to local database")
        }

        emit(Resource.Success(remoteUser))
    }.catch { e -> emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e))) }

    override fun register(
        user: User,
        password: String
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        val userId = firebaseAuthDataSource.register(
            email = user.email,
            password = password,
            name = user.name,
            role = user.role.name
        )
        val userWithId = user.copy(id = userId)
        userDao.insert(userWithId.toEntity())
        // Đăng xuất sau khi tạo tài khoản thành công
        firebaseAuthDataSource.signOut()
        emit(Resource.Success(user))
    }.catch { e -> emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))}

    override fun forgotPassword(email: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        firebaseAuthDataSource.sendPasswordReset(email)
        emit(Resource.Success("Password reset email sent"))
    }.catch { e -> emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e))) }
    override fun signOut(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val currentUserId = firebaseAuthDataSource.getCurrentUserId()
            firebaseAuthDataSource.signOut()
            if (!currentUserId.isNullOrEmpty()) {
                // Phải lấy UserEntity từ Room trước khi xóa
                val userEntityToDelete = userDao.getUserById(currentUserId)

                // Giả định userDao.delete(entity) tồn tại và hoạt động
                if (userEntityToDelete != null) {
                    userDao.delete(userEntityToDelete)
                }
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
        emit(Resource.Success(Unit))
    }
    override fun getCurrentUser(): Flow<Resource<User?>> = flow {
        emit(Resource.Loading())
        try {
            val currentUserId = firebaseAuthDataSource.getCurrentUserId()
            Log.d("AuthRepositoryImpl", "getCurrentUser() called")
            Log.d("AuthRepositoryImpl", "Current user ID: $currentUserId")

            if (currentUserId.isNullOrEmpty()) {
                emit(Resource.Success(null))
                return@flow
            }

            // 1. Ưu tiên lấy từ Local cache
            val localUserEntity = userDao.getUserById(currentUserId) // Giả định userDao.getUserById(id) tồn tại

            if (localUserEntity != null) {
                emit(Resource.Success(localUserEntity.toDomain()))
            } else {
                // 2. Nếu không có trong Local, lấy từ Remote và cache lại
                val remoteUser = firebaseAuthDataSource.getUserProfile(currentUserId)
                userDao.insert(remoteUser.toEntity())
                emit(Resource.Success(remoteUser))
            }
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }
    override fun isUserLoggedIn(): Flow<Resource<Boolean>> = flow {
        try {
            val isLoggedIn = firebaseAuthDataSource.getCurrentUserId() != null
            emit(Resource.Success(isLoggedIn))
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }
}
