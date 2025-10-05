package com.example.datn.data.repository.impl


import com.example.datn.core.network.FirebaseAuthDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
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

    override fun login(email: String, password: String , role: UserRole): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        val userId = firebaseAuthDataSource.login(email, password , role.name)
        val remoteUser = firebaseAuthDataSource.getUserProfile(userId)

        if(userDao.isUserExists(remoteUser.id)){
            userDao.insertUser(remoteUser.toEntity())
        }
        emit(Resource.Success(remoteUser))
    }.catch { e -> emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e))) }

    override fun register(
        email: String,
        password: String,
        name: String,
        role: UserRole
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        val userId = firebaseAuthDataSource.register(email, password, name, role.name)
        val user = User(userId, email, name, UserRole.valueOf(role.name))
        // Đăng xuất sau khi tạo tài khoản thành công
        firebaseAuthDataSource.signOut()
        emit(Resource.Success(user))
    }.catch { e -> emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))}

    override fun forgotPassword(email: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        firebaseAuthDataSource.sendPasswordReset(email)
        emit(Resource.Success("Password reset email sent"))
    }.catch { e -> emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e))) }
}
