package com.example.datn.core.network.service.lesson

import android.util.Log
import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.LessonContent
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

private const val TAG = "LessonContentService"

class LessonContentService @Inject constructor() :
    BaseFirestoreService<LessonContent>(
        collectionName = "lesson_contents",
        clazz = LessonContent::class.java
    ) {

    /**
     * Lấy tất cả nội dung của bài học
     */
    suspend fun getContentByLesson(lessonId: String): List<LessonContent> {
        Log.d(TAG, "Fetching contents for lesson: $lessonId")
        return try {
            val snapshot = collectionRef
                .whereEqualTo("lessonId", lessonId)
                .orderBy("order")
                .get()
                .await()

            val contents = snapshot.documents.mapNotNull {
                try {
                    it.internalToDomain(clazz)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse content doc ${it.id}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${contents.size} contents for lesson $lessonId")
            contents
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching contents by lesson: $lessonId", e)
            emptyList()
        }
    }

    /**
     * Thêm nội dung mới
     */
    suspend fun addContent(content: LessonContent): LessonContent? {
        Log.d(TAG, "Adding new content: ${content.title}")
        return try {
            val docRef = if (content.id.isNotEmpty()) {
                collectionRef.document(content.id)
            } else {
                collectionRef.document()
            }

            val now = Instant.now()
            val contentWithId = content.copy(
                id = docRef.id,
                createdAt = now,
                updatedAt = now
            )

            docRef.set(contentWithId).await()
            Log.i(TAG, "Successfully added content: ${contentWithId.title} (ID: ${contentWithId.id})")
            contentWithId
        } catch (e: Exception) {
            Log.e(TAG, "Error adding content: ${content.title}", e)
            null
        }
    }

    /**
     * Cập nhật nội dung
     */
    suspend fun updateContent(contentId: String, content: LessonContent): Boolean {
        Log.d(TAG, "Updating content: $contentId")
        return try {
            val updatedContent = content.copy(
                id = contentId,
                updatedAt = Instant.now()
            )
            collectionRef.document(contentId).set(updatedContent).await()
            Log.i(TAG, "Successfully updated content: $contentId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating content: $contentId", e)
            false
        }
    }

    /**
     * Xóa nội dung
     */
    suspend fun deleteContent(contentId: String): Boolean {
        Log.d(TAG, "Deleting content: $contentId")
        return try {
            collectionRef.document(contentId).delete().await()
            Log.i(TAG, "Successfully deleted content: $contentId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting content: $contentId", e)
            false
        }
    }

    /**
     * Lấy nội dung theo ID
     */
    suspend fun getContentById(contentId: String): LessonContent? {
        Log.d(TAG, "Fetching content by ID: $contentId")
        return try {
            val doc = collectionRef.document(contentId).get().await()
            if (doc.exists()) {
                doc.internalToDomain(clazz)
            } else {
                Log.w(TAG, "Content not found for ID: $contentId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching content by ID: $contentId", e)
            null
        }
    }
}