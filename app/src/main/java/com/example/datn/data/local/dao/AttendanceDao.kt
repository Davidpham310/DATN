package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.AttendanceEntity

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendances WHERE classId = :classId AND date = :date")
    suspend fun getForClassOnDate(classId: String, date: String): List<AttendanceEntity>
}
