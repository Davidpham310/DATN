package com.example.datn.data.repository.impl

import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.mapper.internalToFirestoreMap
import com.example.datn.data.local.dao.DailyStudyTimeDao
import com.example.datn.data.local.dao.StudentLessonProgressDao
import com.example.datn.data.mapper.*
import com.example.datn.domain.models.DailyStudyTime
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.IProgressRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val studentLessonProgressDao: StudentLessonProgressDao,
    private val dailyStudyTimeDao: DailyStudyTimeDao,
    private val firestore: FirebaseFirestore
) : IProgressRepository {

    override fun getLessonProgress(
        studentId: String,
        lessonId: String
    ): Flow<Resource<StudentLessonProgress?>> = flow {
        emit(Resource.Loading())
        try {
            val entity = studentLessonProgressDao.getProgressByStudentAndLesson(studentId, lessonId)
            val progress = entity?.toDomain()
            emit(Resource.Success(progress))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi lấy tiến độ bài học"))
        }
    }

    override fun updateLessonProgress(progress: StudentLessonProgress): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Sử dụng REPLACE insert để upsert tiến độ bài học vào Room
            studentLessonProgressDao.insert(progress.toEntity())
            android.util.Log.d("ProgressRepositoryImpl", "✅ Saved to Room: student_lesson_progress")
            android.util.Log.d("ProgressRepositoryImpl", "   - Student ID: ${progress.studentId}")
            android.util.Log.d("ProgressRepositoryImpl", "   - Lesson ID: ${progress.lessonId}")
            android.util.Log.d("ProgressRepositoryImpl", "   - Progress: ${progress.progressPercentage}%")

            // Đồng bộ thêm lên Firestore
            val map = internalToFirestoreMap(progress, StudentLessonProgress::class.java)
            android.util.Log.d("ProgressRepositoryImpl", "📤 Uploading to Firestore: student_lesson_progress")
            firestore.collection("student_lesson_progress")
                .document(progress.id)
                .set(map)
                .await()
            android.util.Log.d("ProgressRepositoryImpl", "✅ Created/Updated Firestore collection: student_lesson_progress")
            android.util.Log.d("ProgressRepositoryImpl", "   - Document ID: ${progress.id}")
            android.util.Log.d("ProgressRepositoryImpl", "   - Collection path: student_lesson_progress/${progress.id}")

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("ProgressRepositoryImpl", "❌ Error updating lesson progress: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Lỗi cập nhật tiến độ bài học"))
        }
    }

    override fun logDailyStudyTime(
        studentId: String,
        date: LocalDate,
        durationSeconds: Long
    ): Flow<Resource<DailyStudyTime>> = flow {
        emit(Resource.Loading())
        try {
            val now = Instant.now()

            // Lấy bản ghi hiện tại (nếu có) rồi cộng dồn thời gian học
            val existingEntity = dailyStudyTimeDao.getDailyTimeByStudentAndDate(studentId, date)

            val resultDomain: DailyStudyTime = if (existingEntity != null) {
                val current = existingEntity.toDomain()
                val updated = current.copy(
                    durationSeconds = current.durationSeconds + durationSeconds,
                    updatedAt = now
                )
                dailyStudyTimeDao.insert(updated.toEntity())
                updated
            } else {
                val created = DailyStudyTime(
                    id = UUID.randomUUID().toString(),
                    studentId = studentId,
                    date = date,
                    durationSeconds = durationSeconds,
                    createdAt = now,
                    updatedAt = now
                )
                dailyStudyTimeDao.insert(created.toEntity())
                created
            }

            // Đồng bộ thêm thống kê thời gian học lên Firestore
            val map = internalToFirestoreMap(resultDomain, DailyStudyTime::class.java)
            android.util.Log.d("ProgressRepositoryImpl", "📤 Uploading to Firestore: student_daily_study_time")
            android.util.Log.d("ProgressRepositoryImpl", "   - Student ID: ${resultDomain.studentId}")
            android.util.Log.d("ProgressRepositoryImpl", "   - Date: ${resultDomain.date}")
            android.util.Log.d("ProgressRepositoryImpl", "   - Duration: ${resultDomain.durationSeconds}s")
            firestore.collection("student_daily_study_time")
                .document(resultDomain.id)
                .set(map)
                .await()
            android.util.Log.d("ProgressRepositoryImpl", "✅ Created/Updated Firestore collection: student_daily_study_time")
            android.util.Log.d("ProgressRepositoryImpl", "   - Document ID: ${resultDomain.id}")
            android.util.Log.d("ProgressRepositoryImpl", "   - Collection path: student_daily_study_time/${resultDomain.id}")

            emit(Resource.Success(resultDomain))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("ProgressRepositoryImpl", "❌ Error logging daily study time: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Lỗi ghi lại thời gian học"))
        }
    }

    override fun getDailyStudyTime(
        studentId: String,
        date: LocalDate
    ): Flow<Resource<DailyStudyTime?>> = flow {
        emit(Resource.Loading())
        try {
            val entity = dailyStudyTimeDao.getDailyTimeByStudentAndDate(studentId, date)
            val dailyTime = entity?.toDomain()
            emit(Resource.Success(dailyTime))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi lấy thời gian học"))
        }
    }

    override fun getAllDailyStudyTime(
        studentId: String
    ): Flow<Resource<List<DailyStudyTime>>> = flow {
        emit(Resource.Loading())
        try {
            val entities = dailyStudyTimeDao.getAllByStudent(studentId)
            val dailyTimes = entities.map { it.toDomain() }
            emit(Resource.Success(dailyTimes))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi lấy thống kê thời gian học"))
        }
    }

    override fun getProgressOverview(studentId: String): Flow<Resource<List<StudentLessonProgress>>> = flow {
        emit(Resource.Loading())
        try {
            val entities = studentLessonProgressDao.getAllProgressByStudent(studentId)
            val progressList = entities.map { it.toDomain() }
            emit(Resource.Success(progressList))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Lỗi lấy tổng quan tiến độ"))
        }
    }
}
