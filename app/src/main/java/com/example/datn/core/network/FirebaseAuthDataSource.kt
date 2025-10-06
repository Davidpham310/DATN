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

    // 🔹 Đăng nhập
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

    // 🔹 Đăng ký (kiểm tra tồn tại trước khi thêm)
    suspend fun register(email: String, password: String, name: String, role: String): String {
        // 1️⃣ Kiểm tra xem email đã tồn tại trong Firestore chưa
        val existingUser = usersCollection
            .whereEqualTo("email", email)
            .get()
            .await()

        if (!existingUser.isEmpty) {
            throw Exception("Email đã tồn tại trong hệ thống.")
        }

        // 2️⃣ Nếu chưa có thì tạo tài khoản trong Firebase Auth
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = result.user?.uid ?: throw Exception("Không thể tạo tài khoản.")

        // 3️⃣ Tạo dữ liệu người dùng để lưu vào Firestore
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

        // 4️⃣ Lưu lên Firestore
        usersCollection.document(userId).set(userData).await()

        return userId
    }

    // 🔹 Lấy thông tin người dùng từ Firestore
    suspend fun getUserProfile(userId: String): User {
        val snapshot = usersCollection.document(userId).get().await()

        if (!snapshot.exists()) {
            throw Exception("Không tìm thấy thông tin người dùng.")
        }

        val data = snapshot.data ?: throw Exception("Dữ liệu người dùng bị lỗi.")

        return User(
            id = data["id"] as String,
            email = data["email"] as String,
            name = data["name"] as String,
            role = UserRole.valueOf((data["role"] as String).uppercase()),
            avatarUrl = data["avatarUrl"] as String?,
            phone = data["phone"] as String?,
            createdAt = data["createdAt"] as Long?,
            updatedAt = data["updatedAt"] as Long?
        )
    }

    // 🔹 Gửi email reset mật khẩu
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    // 🔹 Lưu hồ sơ người dùng (nếu cần cập nhật)
    suspend fun saveUserProfile(user: User) {
        usersCollection.document(user.id).set(
            mapOf(
                "id" to user.id,
                "email" to user.email,
                "name" to user.name,
                "role" to user.role.name,
                "avatarUrl" to user.avatarUrl,
                "phone" to user.phone,
                "createdAt" to user.createdAt,
                "updatedAt" to user.updatedAt
            )
        ).await()
    }

    // 🔹 Đăng xuất
    fun signOut() {
        auth.signOut()
    }
}
