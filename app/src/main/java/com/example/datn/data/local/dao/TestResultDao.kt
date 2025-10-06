package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.TestResultEntity

@Dao
interface TestResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(r: TestResultEntity)

    @Query("SELECT * FROM test_results WHERE studentId = :studentId")
    suspend fun getResultsForStudent(studentId: String): List<TestResultEntity>
}
