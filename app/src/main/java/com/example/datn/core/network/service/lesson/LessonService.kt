package com.example.datn.core.network.service.lesson

import android.util.Log
import com.example.datn.core.network.service.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Lesson
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

private const val TAG = "LessonService"

class LessonService @Inject constructor() :
    BaseFirestoreService<Lesson>(
        collectionName = "lessons",
        clazz = Lesson::class.java
    ) {

    /**
     * Lấy tất cả bài học của một lớp học
     */
    suspend fun getLessonsByClass(classId: String): List<Lesson> {
        Log.d(TAG, "Fetching lessons for class: $classId")
        return try {
            val snapshot = collectionRef
                .whereEqualTo("classId", classId)
//                .orderBy("order")
                .get()
                .await()

            val lessons = snapshot.documents.mapNotNull {
                try {
                    it.internalToDomain(clazz)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse lesson doc ${it.id}", e)
                    null
                }
            }

            Log.d(TAG, "Found ${lessons.size} lessons for class $classId")
            lessons
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lessons by class: $classId", e)
            emptyList()
        }
    }

    /**
     * Lấy bài học theo ID
     */
    suspend fun getLessonById(lessonId: String): Lesson? {
        Log.d(TAG, "Fetching lesson by ID: $lessonId")
        return try {
            val doc = collectionRef.document(lessonId).get().await()
            if (doc.exists()) {
                val lesson = doc.internalToDomain(clazz)
                Log.i(TAG, "Successfully fetched lesson: ${lesson?.title}")
                lesson
            } else {
                Log.w(TAG, "Lesson not found for ID: $lessonId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lesson by ID: $lessonId", e)
            null
        }
    }

    /**
     * Thêm bài học mới
     */
    suspend fun addLesson(lesson: Lesson): Lesson? {
        Log.d(TAG, "Adding new lesson: ${lesson.title}")
        return try {
            val docRef = if (lesson.id.isNotEmpty()) {
                collectionRef.document(lesson.id)
            } else {
                collectionRef.document()
            }

            val now = Instant.now()
            val lessonWithId = lesson.copy(
                id = docRef.id,
                teacherId = lesson.teacherId,
                classId = lesson.classId,
                title = lesson.title,
                description = lesson.description,
                contentLink = lesson.contentLink,
                order = lesson.order,
                createdAt = now,
                updatedAt = now
            )

            docRef.set(lessonWithId).await()
            Log.i(TAG, "Successfully added lesson: ${lessonWithId.title} (ID: ${lessonWithId.id})")
            lessonWithId
        } catch (e: Exception) {
            Log.e(TAG, "Error adding lesson: ${lesson.title}", e)
            null
        }
    }

    /**
     * Cập nhật bài học
     */
    suspend fun updateLesson(lessonId: String, lesson: Lesson): Boolean {
        Log.d(TAG, "Updating lesson: $lessonId")
        return try {
            val updatedLesson = lesson.copy(
                id = lessonId,
                updatedAt = Instant.now()
            )
            collectionRef.document(lessonId).set(updatedLesson).await()
            Log.i(TAG, "Successfully updated lesson: $lessonId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating lesson: $lessonId", e)
            false
        }
    }

    /**
     * Xóa bài học
     */
    suspend fun deleteLesson(lessonId: String): Boolean {
        Log.d(TAG, "Deleting lesson: $lessonId")
        return try {
            // Xóa tất cả nội dung bài học trước
            val contentRef = FirebaseFirestore.getInstance().collection("lesson_contents")
            val contents = contentRef
                .whereEqualTo("lessonId", lessonId)
                .get()
                .await()

            firestore.runBatch { batch ->
                contents.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.delete(collectionRef.document(lessonId))
            }.await()

            Log.i(TAG, "Successfully deleted lesson $lessonId and ${contents.size()} contents")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting lesson: $lessonId", e)
            false
        }
    }

    /**
     * Lấy tất cả bài học của giáo viên
     */
    suspend fun getLessonsByTeacher(teacherId: String): List<Lesson> {
        Log.d(TAG, "Fetching lessons for teacher: $teacherId")
        return try {
            val snapshot = collectionRef
                .whereEqualTo("teacherId", teacherId)
                .orderBy("createdAt")
                .get()
                .await()

            snapshot.documents.mapNotNull {
                try {
                    it.internalToDomain(clazz)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse lesson doc ${it.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lessons by teacher: $teacherId", e)
            emptyList()
        }
    }
}