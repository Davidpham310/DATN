package com.example.datn.core.network.service.lesson

import android.util.Log
import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.LessonContent
import com.google.firebase.firestore.FirebaseFirestore
import com.example.datn.core.network.service.minio.MinIOService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject

private const val TAG = "LessonContentService"

class LessonContentService @Inject constructor() :
    BaseFirestoreService<LessonContent>(
        collectionName = "lesson_contents",
        clazz = LessonContent::class.java
    ) {

    suspend fun getContentById(contentId: String): LessonContent? {
        Log.d(TAG, "Fetching content by ID: $contentId")
        return try {
            val doc = collectionRef.document(contentId).get().await()
            if (doc.exists()) {
                val content = doc.internalToDomain(clazz)
                Log.i(TAG, "Successfully fetched content: ${content?.title}")
                content
            } else {
                Log.w(TAG, "Content not found: $contentId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching content by ID: $contentId", e)
            null
        }
    }

    suspend fun getContentByLesson(lessonId: String): List<LessonContent> {
        Log.d(TAG, "Fetching contents for lesson: $lessonId")
        return try {
            val snapshot = collectionRef
                .whereEqualTo("lessonId", lessonId)
//                .orderBy("order")
                .get()
                .await()

            snapshot.documents.mapNotNull {
                try {
                    it.internalToDomain(clazz)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse content doc ${it.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching contents by lesson: $lessonId", e)
            emptyList()
        }
    }

    /**
     * Thêm nội dung mới, nếu có file sẽ upload MinIO
     */
    suspend fun addContent(content: LessonContent, fileStream: InputStream? = null, fileSize: Long = 0): LessonContent? {
        return try {
            val existing = getContentByLesson(content.lessonId)
            val newOrder = if (existing.isEmpty()) 1 else existing.maxOf { it.order } + 1

            val docRef = if (content.id.isNotEmpty()) collectionRef.document(content.id)
            else collectionRef.document()

            val now = Instant.now()
            val contentWithId = content.copy(
                id = docRef.id,
                order = newOrder,
                createdAt = now,
                updatedAt = now
            )

            // Upload file nếu có
            val finalContent = if (fileStream != null) {
                val objectName = "lesson/${content.lessonId}/${content.title}_${System.currentTimeMillis()}"
                withContext(Dispatchers.IO) {
                    MinIOService.uploadFile(objectName, fileStream, fileSize, content.contentType.name)
                }
                contentWithId.copy(content = objectName) // lưu objectName
            } else contentWithId

            docRef.set(finalContent).await()
            finalContent
        } catch (e: Exception) {
            Log.e(TAG, "Error adding content: ${content.title}", e)
            null
        }
    }

    /**
     * Cập nhật nội dung, nếu đổi file sẽ upload mới và xóa file cũ
     */
    suspend fun updateContent(contentId: String, content: LessonContent, newFileStream: InputStream? = null, newFileSize: Long = 0): Boolean {
        return try {
            val doc = collectionRef.document(contentId).get().await()
            val oldContent = doc.internalToDomain(clazz) ?: return false
            val oldOrder = oldContent.order
            val newOrder = content.order

            var updatedContent = content.copy(updatedAt = Instant.now())

            // Nếu có file mới -> upload và xóa file cũ
            if (newFileStream != null) {
                val objectName = "lesson/${content.lessonId}/${content.title}_${System.currentTimeMillis()}"
                withContext(Dispatchers.IO) {
                    MinIOService.uploadFile(objectName, newFileStream, newFileSize, content.contentType.name)
                    // Xóa file cũ nếu tồn tại
                    if (oldContent.content.isNotEmpty()) MinIOService.deleteFile(oldContent.content)
                }
                updatedContent = updatedContent.copy(content = objectName)
            }

            val otherContents = getContentByLesson(content.lessonId).filter { it.id != contentId }
            FirebaseFirestore.getInstance().runBatch { batch ->
                // Hoán đổi order nếu cần
                if (oldOrder != newOrder) {
                    otherContents.find { it.order == newOrder }?.let { conflict ->
                        batch.update(collectionRef.document(conflict.id), "order", oldOrder)
                    }
                }
                batch.set(collectionRef.document(contentId), updatedContent)
            }.await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating content: $contentId", e)
            false
        }
    }

    /**
     * Xóa nội dung và xóa file MinIO nếu có
     */
    suspend fun deleteContent(contentId: String): Boolean {
        return try {
            val doc = collectionRef.document(contentId).get().await()
            val contentToDelete = doc.internalToDomain(clazz) ?: return false
            val deletedOrder = contentToDelete.order

            val otherContents = getContentByLesson(contentToDelete.lessonId).filter { it.id != contentId }
            FirebaseFirestore.getInstance().runBatch { batch ->
                batch.delete(collectionRef.document(contentId))
                otherContents.filter { it.order > deletedOrder }
                    .forEach { batch.update(collectionRef.document(it.id), "order", it.order - 1) }
            }.await()

            // Xóa file MinIO nếu có
            if (contentToDelete.content.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    MinIOService.deleteFile(contentToDelete.content)
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting content: $contentId", e)
            false
        }
    }
}
