package com.example.datn.data.remote.service.lesson

import android.util.Log
import com.example.datn.data.remote.service.firestore.BaseFirestoreService
import com.example.datn.data.remote.service.minio.MinIOService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.LessonContent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject

private const val TAG = "LessonContentService"

class LessonContentService @Inject constructor(
    private val minIOService: MinIOService
) : BaseFirestoreService<LessonContent>(
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
                .get()
                .await()

            snapshot.documents.mapNotNull {
                try {
                    it.internalToDomain(clazz)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse content doc ${it.id}", e)
                    null
                }
            }.sortedBy { it.order }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching contents by lesson: $lessonId", e)
            throw e
        }
    }

    /**
     * Thêm nội dung mới
     * @param content Nội dung cần thêm
     * @param fileStream InputStream của file (nếu có)
     * @param fileSize Kích thước file
     * @return LessonContent đã được tạo với ID và objectName (nếu có file)
     */
    suspend fun addContent(
        content: LessonContent,
        fileStream: InputStream? = null,
        fileSize: Long = 0,
        contentType: String = "application/octet-stream"
    ): LessonContent? {
        return try {
            val existing = getContentByLesson(content.lessonId)
            val newOrder = if (existing.isEmpty()) 1 else existing.maxOf { it.order } + 1
            val docRef = if (content.id.isNotEmpty()) collectionRef.document(content.id) else collectionRef.document()

            val now = Instant.now()
            var finalContent = content.copy(
                id = docRef.id,
                order = newOrder,
                createdAt = now,
                updatedAt = now
            )

            // 🟡 Nếu có file, upload lên MinIO và cập nhật URL
            if (fileStream != null) {
                val objectName = "lesson_contents/${docRef.id}"
                minIOService.uploadFile(objectName, fileStream, fileSize, contentType)
                val fileUrl = minIOService.getFileUrl(objectName)
                finalContent = finalContent.copy(content = fileUrl)
            }

            docRef.set(finalContent).await()
            Log.i(TAG, "✅ Added Firestore + uploaded MinIO: ${finalContent.title}")
            finalContent
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding content Firestore/MinIO: ${content.title}", e)
            null
        }
    }

    /**
     * Cập nhật nội dung
     * @param contentId ID của content cần update
     * @param content Dữ liệu mới
     * @param newFileStream InputStream của file mới (nếu có)
     * @param newFileSize Kích thước file mới
     */
    suspend fun updateContent(
        contentId: String,
        updatedContent: LessonContent,
        newFileStream: InputStream? = null,
        newFileSize: Long = 0,
        contentType: String = "application/octet-stream"
    ): Boolean {
        return try {
            var finalContent = updatedContent

            // 🟡 Nếu có file mới => upload lại
            if (newFileStream != null) {
                val objectName = "lesson_contents/$contentId"
                if (minIOService.fileExists(objectName)) {
                    minIOService.deleteFile(objectName)
                }
                minIOService.uploadFile(objectName, newFileStream, newFileSize, contentType)
                val newUrl = minIOService.getFileUrl(objectName)
                finalContent = updatedContent.copy(content = newUrl)
            }

            collectionRef.document(contentId).set(finalContent).await()
            Log.i(TAG, "✅ Updated Firestore content: ${updatedContent.title}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating Firestore/MinIO content: $contentId", e)
            false
        }
    }

    /**
     * Xóa nội dung và file MinIO nếu có
     */
    suspend fun deleteContent(contentId: String): Boolean {
        return try {
            val doc = collectionRef.document(contentId).get().await()
            val contentToDelete = doc.internalToDomain(clazz) ?: return false

            val objectName = "lesson_contents/$contentId"
            if (minIOService.fileExists(objectName)) {
                minIOService.deleteFile(objectName)
            }

            val deletedOrder = contentToDelete.order
            val otherContents = getContentByLesson(contentToDelete.lessonId)
                .filter { it.id != contentId }

            FirebaseFirestore.getInstance().runBatch { batch ->
                batch.delete(collectionRef.document(contentId))
                otherContents.filter { it.order > deletedOrder }.forEach {
                    batch.update(collectionRef.document(it.id), "order", it.order - 1)
                }
            }.await()

            Log.i(TAG, "✅ Deleted Firestore + MinIO: ${contentToDelete.title}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting Firestore/MinIO content: $contentId", e)
            false
        }
    }
    suspend fun getContentUrl(content: LessonContent, expirySeconds: Int = 3600): String {
        if (content.content.isEmpty()) {
            throw IllegalArgumentException("Content path is empty")
        }
        val objectName = "lesson_contents/${content.id}"
        return minIOService.getFileUrl(objectName, expirySeconds)
    }

    suspend fun getDirectFileUrl(content: LessonContent): String {
        if (content.content.isEmpty()) {
            throw IllegalArgumentException("Content path is empty")
        }
        val objectName = "lesson_contents/${content.id}"
        return minIOService.getDirectFileUrl(objectName)
    }
}