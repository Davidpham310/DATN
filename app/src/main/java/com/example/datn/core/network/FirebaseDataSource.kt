package com.example.datn.core.network

import com.example.datn.core.base.BaseDataSource
import com.example.datn.core.utils.Resource
import javax.inject.Inject

class FirebaseDataSource @Inject constructor(
    private val firebaseService: FirebaseService
) : BaseDataSource() {

    suspend fun login(email: String, password: String): Resource<String> =
        try {
            val result = firebaseService.login(email, password)
            if (result.isSuccess) {
                Resource.Success(result.getOrNull()!!)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }

    suspend fun register(email: String, password: String): Resource<String> =
        try {
            val result = firebaseService.register(email, password)
            if (result.isSuccess) {
                Resource.Success(result.getOrNull()!!)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Register failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Register failed")
        }

    suspend fun resetPassword(email: String): Resource<Unit> =
        try {
            val result = firebaseService.resetPassword(email)
            if (result.isSuccess) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Reset password failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Reset password failed")
        }
}
