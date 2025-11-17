package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.DailyStudyTimeEntity
import java.time.LocalDate

@Dao
interface DailyStudyTimeDao : BaseDao<DailyStudyTimeEntity> {
    @Query("SELECT * FROM daily_study_time WHERE studentId = :studentId AND date = :date")
    suspend fun getDailyTimeByStudentAndDate(studentId: String, date: LocalDate): DailyStudyTimeEntity?

    @Query("SELECT * FROM daily_study_time WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getAllByStudent(studentId: String): List<DailyStudyTimeEntity>
}