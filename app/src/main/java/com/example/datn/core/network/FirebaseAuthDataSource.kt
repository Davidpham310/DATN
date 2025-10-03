package com.example.datn.core.network

import com.example.datn.core.base.BaseDataSource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : BaseDataSource() {

    private val usersCollection = firestore.collection("users")

    // Đăng nhập
    suspend fun login(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw Exception("Login failed")
    }

    // Đăng ký
    suspend fun register(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw Exception("Register failed")
    }

    // Gửi email để đặt lại mật khẩu
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    // Lưu thông tin user vào Firestore
    suspend fun saveUserProfile(user: User) {
        usersCollection.document(user.id)
            .set(
                mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "name" to user.name,
                    "role" to user.role.name
                )
            ).await()
    }

    // Lấy thông tin user từ Firestore
    suspend fun getUserProfile(userId: String): User {
        val snapshot = usersCollection.document(userId).get().await()
        if (!snapshot.exists()) {
            throw Exception("User profile not found")
        }
        val data = snapshot.data ?: throw Exception("User profile is empty")

        return User(
            id = data["id"] as String,
            email = data["email"] as String,
            name = data["name"] as String,
            role = UserRole.valueOf(data["role"] as String)
        )
    }

}