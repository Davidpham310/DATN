package com.example.datn.data.repository.impl

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.ISplashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SplashRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : ISplashRepository {

    override fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())

        val users = userDao.getAllUsers()
        val user = users.firstOrNull()?.toDomain()
        Log.d("SplashRepositoryImpl" , "$user")
        if (user != null) {
            emit(Resource.Success(user))
        } else {
            emit(Resource.Error("No user found"))
        }
    }
}
