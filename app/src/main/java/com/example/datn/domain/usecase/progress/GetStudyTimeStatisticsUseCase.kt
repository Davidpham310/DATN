package com.example.datn.domain.usecase.progress

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.DailyStudyTime
import com.example.datn.domain.models.StudyTimeStatistics
import com.example.datn.domain.repository.IProgressRepository
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetStudyTimeStatisticsUseCase @Inject constructor(
    private val repository: IProgressRepository
) {
    operator fun invoke(studentId: String): Flow<Resource<StudyTimeStatistics>> {
        return repository.getAllDailyStudyTime(studentId).map { resource ->
            when (resource) {
                is Resource.Loading -> Resource.Loading()
                is Resource.Error -> Resource.Error(resource.message)
                is Resource.Success -> {
                    val records = resource.data ?: emptyList()
                    Resource.Success(calculateStatistics(studentId, records))
                }
            }
        }
    }

    private fun calculateStatistics(
        studentId: String,
        records: List<DailyStudyTime>
    ): StudyTimeStatistics {
        val today = LocalDate.now()
        val startOfWeek = today.with(DayOfWeek.MONDAY)
        val startOfMonth = today.withDayOfMonth(1)

        var todaySeconds = 0L
        var weekSeconds = 0L
        var monthSeconds = 0L
        var totalSeconds = 0L

        records.forEach { record ->
            val seconds = record.durationSeconds
            totalSeconds += seconds

            if (record.date == today) {
                todaySeconds += seconds
            }
            if (!record.date.isBefore(startOfWeek)) {
                weekSeconds += seconds
            }
            if (!record.date.isBefore(startOfMonth)) {
                monthSeconds += seconds
            }
        }

        // Sort records by date ascending for chart
        val sortedRecords = records.sortedBy { it.date }

        return StudyTimeStatistics(
            studentId = studentId,
            todaySeconds = todaySeconds,
            weekSeconds = weekSeconds,
            monthSeconds = monthSeconds,
            totalSeconds = totalSeconds,
            dailyRecords = sortedRecords
        )
    }
}
