package com.example.datn.domain.usecase.progress

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.repository.IProgressRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private const val MINIMUM_COMPLETION_TIME_SECONDS = 60L

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
    operator fun invoke(params: UpdateLessonProgressParams): Flow<Resource<StudentLessonProgress>> =
        repository.getLessonProgress(params.studentId, params.lessonId)
            .flatMapLatest { existingRes ->
                val existing = (existingRes as? Resource.Success)?.data

                when (existingRes) {
                    is Resource.Loading -> {
                        // Đang tải tiến độ hiện tại
                        flowOf(Resource.Loading())
                    }
                    is Resource.Success, is Resource.Error -> {
                        val now = Instant.now()
                        val safeAdditionalSeconds = params.additionalTimeSeconds.coerceAtLeast(0)

                        val previousTime = existing?.timeSpentSeconds ?: 0L
                        val newTimeSpent = previousTime + safeAdditionalSeconds
                        val shouldBeCompleted =
                            params.progressPercentage >= 100 && newTimeSpent >= MINIMUM_COMPLETION_TIME_SECONDS

                        val newProgress = if (existing != null) {
                            existing.copy(
                                progressPercentage = params.progressPercentage,
                                lastAccessedContentId = params.lastAccessedContentId,
                                lastAccessedAt = now,
                                isCompleted = shouldBeCompleted || existing.isCompleted,
                                timeSpentSeconds = newTimeSpent,
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
                                isCompleted = shouldBeCompleted,
                                timeSpentSeconds = newTimeSpent,
                                createdAt = createdAt,
                                updatedAt = now
                            )
                        }

                        // Upsert lesson progress (Room + Firestore)
                        repository.updateLessonProgress(newProgress).flatMapLatest { updateRes ->
                            when (updateRes) {
                                is Resource.Loading -> {
                                    flowOf(Resource.Loading())
                                }
                                is Resource.Error -> {
                                    val message = updateRes.message ?: "Lỗi cập nhật tiến độ bài học"
                                    flowOf(Resource.Error(message))
                                }
                                is Resource.Success -> {
                                    // Ghi thêm thống kê thời gian học hằng ngày
                                    if (safeAdditionalSeconds > 0) {
                                        repository.logDailyStudyTime(
                                            params.studentId,
                                            LocalDate.now(),
                                            safeAdditionalSeconds
                                        ).map { dailyRes ->
                                            when (dailyRes) {
                                                is Resource.Loading -> Resource.Loading()
                                                // Nếu log daily time lỗi, vẫn coi như cập nhật tiến độ thành công
                                                is Resource.Error -> Resource.Success(newProgress)
                                                is Resource.Success -> Resource.Success(newProgress)
                                            }
                                        }
                                    } else {
                                        flowOf(Resource.Success(newProgress))
                                    }
                                }
                            }
                        }
                    }
                }
            }
}
