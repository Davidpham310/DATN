package com.example.datn.domain.usecase.progress

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.IProgressRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

data class UpdateLessonProgressParams(
    val studentId: String,
    val lessonId: String,
    val progressPercentage: Int,
    val lastAccessedContentId: String?,
    val additionalTimeSeconds: Long
)

class UpdateLessonProgressUseCase @Inject constructor(
    private val repository: IProgressRepository
) {
    operator fun invoke(params: UpdateLessonProgressParams): Flow<Resource<StudentLessonProgress>> = flow {
        emit(Resource.Loading())
        try {
            val now = Instant.now()
            val existingRes = repository.getLessonProgress(params.studentId, params.lessonId).first()
            val existing = (existingRes as? Resource.Success)?.data

            val safeAdditionalSeconds = params.additionalTimeSeconds.coerceAtLeast(0)

            val newProgress = if (existing != null) {
                existing.copy(
                    progressPercentage = params.progressPercentage,
                    lastAccessedContentId = params.lastAccessedContentId,
                    lastAccessedAt = now,
                    isCompleted = params.progressPercentage >= 100 || existing.isCompleted,
                    timeSpentSeconds = existing.timeSpentSeconds + safeAdditionalSeconds,
                    updatedAt = now
                )
            } else {
                val createdAt = now
                StudentLessonProgress(
                    id = UUID.randomUUID().toString(),
                    studentId = params.studentId,
                    lessonId = params.lessonId,
                    progressPercentage = params.progressPercentage,
                    lastAccessedContentId = params.lastAccessedContentId,
                    lastAccessedAt = now,
                    isCompleted = params.progressPercentage >= 100,
                    timeSpentSeconds = safeAdditionalSeconds,
                    createdAt = createdAt,
                    updatedAt = now
                )
            }

            // Upsert lesson progress
            repository.updateLessonProgress(newProgress).first()

            // Log daily study time for today
            if (safeAdditionalSeconds > 0) {
                repository.logDailyStudyTime(
                    params.studentId,
                    LocalDate.now(),
                    safeAdditionalSeconds
                ).first()
            }

            emit(Resource.Success(newProgress))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi cập nhật tiến độ bài học"))
        }
    }
}
