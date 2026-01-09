package com.example.datn.data.remote.service.user

import android.util.Log
import com.example.datn.data.remote.service.firestore.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

val TAG = "UserService"
class UserService @Inject constructor() :
    BaseFirestoreService<User>(
        collectionName = "users",
        clazz = User::class.java
    ) {
    // Lấy user theo id
    suspend fun getUserById(userId: String): User? {
        val snapshot = collectionRef
            .whereEqualTo("id", userId)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull()
        return doc?.let { it.internalToDomain(clazz) }
    }

    // Lấy user theo email
    suspend fun getUserByEmail(email: String): User? {
        val snapshot = collectionRef
            .whereEqualTo("email", email)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull()
        return doc?.let { it.internalToDomain(clazz) }
    }

    // Cập nhật avatar
    suspend fun updateAvatar(userId: String, avatarUrl: String) {
        collectionRef.document(userId)
            .update("avatarUrl", avatarUrl)
            .await()
    }

    // Lấy tất cả user theo role
    suspend fun getUsersByRole(role: String): List<User> {
        Log.d(TAG, "Querying users with role: $role")
        val snapshot = collectionRef
            .whereEqualTo("role", role)
            .get()
            .await()
        Log.d(TAG, "Firestore returned ${snapshot.size()} documents with role: $role")
        
        if (snapshot.isEmpty) {
            Log.w(TAG, "No documents found with role: $role")
            // Log thêm để debug
            val allDocs = collectionRef.limit(5).get().await()
            Log.d(TAG, "Sample documents in collection (first 5):")
            allDocs.documents.forEach { doc ->
                Log.d(TAG, "  Doc ${doc.id}: role=${doc.getString("role")}, name=${doc.getString("name")}")
            }
        }
        
        val users = snapshot.documents.mapNotNull { doc ->
            try {
                Log.d(TAG, "Processing document ${doc.id}")
                Log.d(TAG, "  Document data: ${doc.data}")
                Log.d(TAG, "  Role field value: ${doc.getString("role")}")
                Log.d(TAG, "  CreatedAt field value: ${doc.get("createdAt")} (type: ${doc.get("createdAt")?.javaClass?.simpleName})")
                Log.d(TAG, "  UpdatedAt field value: ${doc.get("updatedAt")} (type: ${doc.get("updatedAt")?.javaClass?.simpleName})")
                
                val user = doc.internalToDomain(clazz)
                Log.d(TAG, "  ✓ Successfully mapped user: ${user.id} - ${user.name} - ${user.role.name}")
                Log.d(TAG, "  User details: email=${user.email}, isActive=${user.isActive}, createdAt=${user.createdAt}, updatedAt=${user.updatedAt}")
                user
            } catch (e: Exception) {
                Log.e(TAG, "  ✗ Error mapping document ${doc.id}: ${e.message}", e)
                Log.e(TAG, "  Document data: ${doc.data}")
                Log.e(TAG, "  Exception stack trace:", e)
                null
            }
        }
        
        Log.d(TAG, "Total successfully mapped users: ${users.size} out of ${snapshot.size()} documents")
        return users
    }
}