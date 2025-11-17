package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.DailyStudyTime
import com.example.datn.domain.models.StudentLessonProgress
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface IProgressRepository {
    fun getLessonProgress(studentId: String, lessonId: String): Flow<Resource<StudentLessonProgress?>>
    fun updateLessonProgress(progress: StudentLessonProgress): Flow<Resource<Unit>>
    fun logDailyStudyTime(studentId: String, date: LocalDate, durationSeconds: Long): Flow<Resource<DailyStudyTime>>
    fun getDailyStudyTime(studentId: String, date: LocalDate): Flow<Resource<DailyStudyTime?>>
    fun getProgressOverview(studentId: String): Flow<Resource<List<StudentLessonProgress>>>
    fun getAllDailyStudyTime(studentId: String): Flow<Resource<List<DailyStudyTime>>>
}