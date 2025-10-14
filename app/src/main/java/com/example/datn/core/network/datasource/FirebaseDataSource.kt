package com.example.datn.core.network.datasource

import com.example.datn.core.base.BaseDataSource
import com.example.datn.core.network.service.user.UserService
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import javax.inject.Inject

class FirebaseDataSource @Inject constructor(
    private val userService: UserService
) : BaseDataSource() {

    suspend fun getUserById(userId: String): Resource<User?> = safeCallWithResult {
        val user = userService.getUserById(userId)
        user
    }.toResource()

    suspend fun getUserByEmail(email: String): Resource<User?> = safeCallWithResult {
        userService.getUserByEmail(email)
    }.toResource()

    suspend fun getUsersByRole(role: String): Resource<List<User>> = safeCallWithResult {
        userService.getUsersByRole(role)
    }.toResource()

    suspend fun getAllUsers(): Resource<List<User>> = safeCallWithResult {
        userService.getAll()
    }.toResource()

    suspend fun addUser(user: User, id: String? = null): Resource<String> = safeCallWithResult {
        userService.add(id, user)
    }.toResource()

    suspend fun updateUser(id: String, user: User): Resource<Unit> = safeCallWithResult {
        userService.update(id, user)
    }.toResource()

    suspend fun deleteUser(userId: String): Resource<Unit> = safeCallWithResult {
        userService.delete(userId)
    }.toResource()

    suspend fun updateAvatar(userId: String, avatarUrl: String): Resource<Unit> = safeCallWithResult {
        userService.updateAvatar(userId, avatarUrl)
    }.toResource()

    // Helper để chuyển Result<T> thành Resource<T>
    private fun <T> Result<T>.toResource(): Resource<T> {
        return if (this.isSuccess) Resource.Success(this.getOrThrow())
        else Resource.Error(this.exceptionOrNull()?.message ?: "Unknown Firebase Error")
    }
}
