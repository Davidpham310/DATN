package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.ScheduleEntity

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE classId = :classId ORDER BY startTime")
    suspend fun getForClass(classId: String): List<ScheduleEntity>
}
