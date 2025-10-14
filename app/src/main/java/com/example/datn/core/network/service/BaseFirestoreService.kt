package com.example.datn.core.network.service

import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.core.utils.mapper.internalToFirestoreMap
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

abstract class BaseFirestoreService<T : Any>(
    private val collectionName: String,
    protected val clazz: Class<T>
) {
    protected val firestore = FirebaseFirestore.getInstance()
    protected val collectionRef = firestore.collection(collectionName)
    //Helper để chuyển snapshot → entity
    protected fun DocumentSnapshot.toEntity(): T = internalToDomain(clazz)

    open suspend fun add(id: String? = null, data: T): String {
        val map = internalToFirestoreMap(data, clazz)
        return if (id == null) {
            val docRef = collectionRef.add(map).await()
            docRef.id
        } else {
            collectionRef.document(id).set(map).await()
            id
        }
    }

    open suspend fun getById(id: String): T? {
        val snapshot = collectionRef.document(id).get().await()
        return if (snapshot.exists()) snapshot.internalToDomain(clazz) else null
    }

    open suspend fun getAll(): List<T> {
        val snapshot: QuerySnapshot = collectionRef.get().await()
        return snapshot.documents.mapNotNull {
            try {
                it.internalToDomain(clazz)
            } catch (_: Exception) {
                null
            }
        }
    }

    open suspend fun update(id: String, data: T) {
        val map = internalToFirestoreMap(data, clazz)
        collectionRef.document(id).update(map).await()
    }

    open suspend fun delete(id: String) {
        collectionRef.document(id).delete().await()
    }
}
