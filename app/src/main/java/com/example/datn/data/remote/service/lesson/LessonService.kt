package com.example.datn.data.remote.service.lesson

import android.util.Log
import com.example.datn.data.remote.service.firestore.BaseFirestoreService
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
            val existingLessons = getLessonsByClass(lesson.classId)

            // Nếu người dùng không nhập order, tự động tính max + 1
            val desiredOrder = if (lesson.order <= 0) {
                if (existingLessons.isEmpty()) 1 else (existingLessons.maxOf { it.order } + 1)
            } else {
                lesson.order
            }

            // Xác định những bài học cần đẩy order lên
            val lessonsToShift = existingLessons.filter { it.order >= desiredOrder }

            val docRef = if (lesson.id.isNotEmpty()) {
                collectionRef.document(lesson.id)
            } else {
                collectionRef.document()
            }

            val now = Instant.now()
            val lessonWithOrder = lesson.copy(
                id = docRef.id,
                order = desiredOrder,
                createdAt = now,
                updatedAt = now
            )

            // Batch update để đẩy order các bài học trùng
            firestore.runBatch { batch ->
                lessonsToShift.forEach { existingLesson ->
                    batch.update(collectionRef.document(existingLesson.id), "order", existingLesson.order + 1)
                }
                batch.set(docRef, lessonWithOrder)
            }.await()

            Log.i(TAG, "Successfully added lesson: ${lessonWithOrder.title} with order ${lessonWithOrder.order}")
            lessonWithOrder
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
            val doc = collectionRef.document(lessonId).get().await()
            if (!doc.exists()) return false

            val oldLesson = doc.internalToDomain(clazz) ?: return false
            val oldOrder = oldLesson.order
            val newOrder = lesson.order

            if (newOrder == oldOrder) {
                // Nếu order không thay đổi, chỉ cập nhật các thông tin khác
                val updatedLesson = lesson.copy(updatedAt = Instant.now())
                collectionRef.document(lessonId).set(updatedLesson).await()
                Log.i(TAG, "Updated lesson $lessonId without order change")
                return true
            }

            // Lấy danh sách các bài học cùng lớp (ngoại trừ bài hiện tại)
            val otherLessons = getLessonsByClass(lesson.classId).filter { it.id != lessonId }

            firestore.runBatch { batch ->
                // Tìm bài học có order = newOrder để hoán đổi
                otherLessons.find { it.order == newOrder }?.let { conflictLesson ->
                    batch.update(collectionRef.document(conflictLesson.id), "order", oldOrder)
                }

                // Cập nhật bài học hiện tại với order mới
                val updatedLesson = lesson.copy(updatedAt = Instant.now())
                batch.set(collectionRef.document(lessonId), updatedLesson)
            }.await()

            Log.i(TAG, "Successfully updated lesson $lessonId from order $oldOrder to $newOrder")
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
            val doc = collectionRef.document(lessonId).get().await()
            if (!doc.exists()) return false

            val lessonToDelete = doc.internalToDomain(clazz) ?: return false
            val deletedOrder = lessonToDelete.order

            // Xóa nội dung bài học
            val contentRef = FirebaseFirestore.getInstance().collection("lesson_contents")
            val contents = contentRef.whereEqualTo("lessonId", lessonId).get().await()

            // Lấy danh sách các bài học cùng lớp (ngoại trừ bài bị xóa)
            val otherLessons = getLessonsByClass(lessonToDelete.classId).filter { it.id != lessonId }

            firestore.runBatch { batch ->
                // Xóa nội dung bài học
                contents.documents.forEach { batch.delete(it.reference) }

                // Xóa bài học
                batch.delete(collectionRef.document(lessonId))

                // Giảm order các bài học phía sau
                otherLessons.filter { it.order > deletedOrder }
                    .forEach { batch.update(collectionRef.document(it.id), "order", it.order - 1) }
            }.await()

            Log.i(TAG, "Successfully deleted lesson $lessonId and adjusted orders")
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