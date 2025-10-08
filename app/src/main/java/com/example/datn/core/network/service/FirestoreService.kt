package com.example.datn.core.network.service

import com.example.datn.core.utils.mapper.toDomain
import com.example.datn.core.utils.mapper.toFirestoreMap
import com.example.datn.domain.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")
    private val classesCollection = firestore.collection("classes")
    private val lessonsCollection = firestore.collection("lessons")

    // ==========================================================
    // 🔹 USER PROFILE MANAGEMENT
    // ==========================================================

    /**
     * ✅ Lấy thông tin người dùng theo ID.
     * @param userId ID của người dùng.
     * @return [Result.success(User)] nếu tìm thấy, [Result.failure] nếu lỗi.
     */
    suspend fun getUserProfile(userId: String): Result<User> {
        return runCatching {
            val snapshot = usersCollection.document(userId).get().await()
            if (!snapshot.exists()) {
                throw NoSuchElementException("Không tìm thấy người dùng với ID: $userId")
            }
            snapshot.toDomain<User>()
        }
    }

    /**
     * ✅ Tạo hồ sơ người dùng mới khi đăng ký.
     */
    suspend fun createUserProfile(user: User): Result<Unit> {
        return runCatching {
            val userMap = user.toFirestoreMap()
            usersCollection.document(user.id).set(userMap).await()
        }
    }

    /**
     * ✅ Cập nhật thông tin hồ sơ người dùng hiện có.
     * Chỉ cập nhật các trường có giá trị mới (merge thay vì overwrite).
     */
    suspend fun updateUserProfile(user: User): Result<Unit> {
        return runCatching {
            val userMap = user.toFirestoreMap()
            usersCollection.document(user.id)
                .set(userMap, SetOptions.merge())
                .await()
        }
    }

    /**
     * ✅ Xóa người dùng khỏi hệ thống (nếu cần).
     */
    suspend fun deleteUserProfile(userId: String): Result<Unit> {
        return runCatching {
            usersCollection.document(userId).delete().await()
        }
    }

    /**
     * ✅ Kiểm tra xem một email đã tồn tại trong hệ thống chưa.
     */
    suspend fun checkUserExists(email: String): Result<Boolean> {
        return runCatching {
            val snapshot = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        }
    }


}
