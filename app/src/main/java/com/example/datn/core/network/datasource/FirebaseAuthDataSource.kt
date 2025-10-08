package com.example.datn.core.network.datasource

import com.example.datn.core.base.BaseDataSource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : BaseDataSource() {

    private val usersCollection = firestore.collection("users")


    // Đăng nhập
    suspend fun login(email: String, password: String, expectedRole: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val userId = result.user?.uid ?: throw Exception("Không thể đăng nhập.")

        // Lấy thông tin người dùng từ Firestore
        val snapshot = usersCollection.document(userId).get().await()
        if (!snapshot.exists()) {
            throw Exception("Không tìm thấy thông tin người dùng.")
        }

        val actualRole = (snapshot.getString("role") ?: "").uppercase()

        // Kiểm tra vai trò
        if (actualRole != expectedRole.uppercase()) {
            auth.signOut()
            throw Exception("Bạn không có quyền đăng nhập với vai trò này.")
        }

        return userId
    }

    // Đăng ký
    suspend fun register(email: String, password: String, name: String, role: String): String {
        // Kiểm tra xem email đã tồn tại trong Firestore chưa
        val existingUser = usersCollection
            .whereEqualTo("email", email)
            .get()
            .await()

        if (!existingUser.isEmpty) {
            throw Exception("Email đã tồn tại trong hệ thống.")
        }else{
            // Nếu chưa có thì tạo tài khoản trong Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Không thể tạo tài khoản.")

            // Tạo dữ liệu người dùng để lưu vào Firestore
            val userData = hashMapOf(
                "id" to userId,
                "email" to email,
                "name" to name,
                "role" to role.uppercase(),
                "avatarUrl" to "",
                "phone" to "",
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "isActive" to true
            )
            // Lưu lên Firestore
            usersCollection.document(userId).set(userData).await()

            return userId
        }
    }

    // Lấy thông tin người dùng từ Firestore
    suspend fun getUserProfile(userId: String): User {
        val snapshot = usersCollection.document(userId).get().await()

        if (!snapshot.exists()) {
            throw Exception("Không tìm thấy thông tin người dùng.")
        }

        val data = snapshot.data ?: throw Exception("Dữ liệu người dùng bị lỗi.")

        // Lấy dữ liệu và thực hiện chuyển đổi kiểu
        val isActive = data["isActive"] as? Boolean ?: true
        val roleString = (data["role"] as? String)?.uppercase() ?: UserRole.STUDENT.name
        val createdAtMillis = data["createdAt"] as? Long ?: System.currentTimeMillis()
        val updatedAtMillis = data["updatedAt"] as? Long ?: System.currentTimeMillis()

        return User(
            id = data["id"] as String,
            email = data["email"] as String,
            name = data["name"] as String,
            role = UserRole.valueOf(roleString),
            avatarUrl = data["avatarUrl"] as String?,
            isActive = isActive,
            createdAt = Instant.ofEpochMilli(createdAtMillis),
            updatedAt = Instant.ofEpochMilli(updatedAtMillis)
        )
    }

    // Gửi email reset mật khẩu
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Đăng xuất
    fun signOut() {
        auth.signOut()
    }
}