package com.example.datn.core.network

/**
 * File này định nghĩa lớp `FirebaseService`, đóng vai trò là một lớp trung gian (wrapper)
 * để tương tác với các dịch vụ của Firebase, cụ thể ở đây là Firebase Authentication.
 *
 * Công dụng chính:
 * - Cung cấp các phương thức để thực hiện các chức năng xác thực người dùng như đăng nhập (`login`), đăng ký, đăng xuất,...
 * - Sử dụng `suspend` function và `kotlinx.coroutines.tasks.await()` để xử lý các tác vụ bất đồng bộ của Firebase một cách tuần tự và dễ đọc hơn trong coroutines.
 * - Đóng gói logic xử lý lỗi (sử dụng `try-catch` và `Result`) để trả về kết quả thành công hoặc thất bại một cách rõ ràng.
 * - `@Inject constructor` cho thấy lớp này được thiết kế để sử dụng với Hilt (hoặc một thư viện dependency injection khác) để tự động cung cấp đối tượng `FirebaseAuth`.
 */
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FirebaseService @Inject constructor(private val auth: FirebaseAuth) {

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid.orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid.orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}