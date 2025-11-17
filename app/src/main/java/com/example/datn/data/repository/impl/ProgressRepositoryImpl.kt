package com.example.datn.data.repository.impl

import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.DailyStudyTimeDao
import com.example.datn.data.local.dao.StudentLessonProgressDao
import com.example.datn.data.mapper.*
import com.example.datn.domain.models.DailyStudyTime
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.IProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val studentLessonProgressDao: StudentLessonProgressDao,
    private val dailyStudyTimeDao: DailyStudyTimeDao
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
            // Sử dụng REPLACE insert để upsert tiến độ bài học
            studentLessonProgressDao.insert(progress.toEntity())
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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

            emit(Resource.Success(resultDomain))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
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
