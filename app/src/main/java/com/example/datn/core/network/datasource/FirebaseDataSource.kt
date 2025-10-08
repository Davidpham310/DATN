package com.example.datn.core.network.datasource

import com.example.datn.core.base.BaseDataSource
import com.example.datn.core.network.service.FirestoreService
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseDataSource @Inject constructor(
    private val firestoreService: FirestoreService,
) : BaseDataSource() {
    // Hàm Helper chuyển đổi Result<T> thành Resource<T>
    private fun <T> Result<T>.toResource(): Resource<T> {
        return if (this.isSuccess) {
            Resource.Success(this.getOrThrow())
        } else {
            Resource.Error(this.exceptionOrNull()?.message ?: "Unknown Firebase Error")
        }
    }
    // ==========================================================
    // 🔹 USER FIRESTORE OPERATIONS
    // ==========================================================

    /**
     * ✅ Lấy thông tin người dùng từ Firestore theo ID.
     */
    suspend fun getUser(userId: String): User {
        val result = firestoreService.getUserProfile(userId)
        return result.getOrElse { throw it }
    }

    /**
     * ✅ Cập nhật thông tin người dùng.
     */
    suspend fun updateUser(user: User) {
        val result = firestoreService.updateUserProfile(user)
        result.getOrElse { throw it }
    }

    /**
     * ✅ Xóa người dùng khỏi Firestore.
     */
    suspend fun deleteUser(userId: String) {
        val result = firestoreService.deleteUserProfile(userId)
        result.getOrElse { throw it }
    }

    /**
     * ✅ Tạo người dùng mới (ví dụ khi đăng ký).
     */
    suspend fun createUser(user: User) {
        val result = firestoreService.createUserProfile(user)
        result.getOrElse { throw it }
    }

    /**
     * ✅ Kiểm tra xem email đã tồn tại hay chưa.
     */
    suspend fun checkUserExists(email: String): Boolean {
        val result = firestoreService.checkUserExists(email)
        return result.getOrElse { throw it }
    }

}