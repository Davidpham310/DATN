package com.example.datn.domain.usecase.progress

import com.example.datn.domain.repository.IProgressRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Ghi nhận tổng thời gian học trong ngày (DailyStudyTime) cho một học sinh.
 *
 * - Dùng chung cho mọi hoạt động học tập: bài học, bài kiểm tra, minigame...
 * - Không ghi gì nếu durationSeconds <= 0 để tránh tạo record rác.
 */
class LogDailyStudyTimeUseCase @Inject constructor(
    private val repository: IProgressRepository
) {
    suspend operator fun invoke(studentId: String, durationSeconds: Long) {
        val safeDuration = durationSeconds.coerceAtLeast(0)
        if (safeDuration <= 0) return

        // Cộng dồn thời gian học vào bản ghi DailyStudyTime của ngày hôm nay
        repository.logDailyStudyTime(
            studentId = studentId,
            date = LocalDate.now(),
            durationSeconds = safeDuration
        ).first()
    }
}
