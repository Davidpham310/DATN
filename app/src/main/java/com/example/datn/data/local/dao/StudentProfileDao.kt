package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.StudentProfileEntity

@Dao
interface StudentProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: StudentProfileEntity)

    @Query("SELECT * FROM student_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): StudentProfileEntity?

    @Delete
    suspend fun delete(profile: StudentProfileEntity)
}
