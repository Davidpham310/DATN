package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.StudentAchievementEntity

@Dao
interface StudentAchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sa: StudentAchievementEntity)

    @Query("SELECT * FROM student_achievements WHERE studentId = :studentId")
    suspend fun getForStudent(studentId: String): List<StudentAchievementEntity>
}
