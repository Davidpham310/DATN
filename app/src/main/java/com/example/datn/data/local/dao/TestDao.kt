package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.TestEntity

@Dao
interface TestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: TestEntity)

    @Query("SELECT * FROM tests WHERE classId = :classId")
    suspend fun getTestsForClass(classId: String): List<TestEntity>

    @Delete
    suspend fun deleteTest(test: TestEntity)
}
