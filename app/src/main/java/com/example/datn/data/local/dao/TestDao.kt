package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.TestEntity

@Dao
interface TestDao : BaseDao<TestEntity> {
    @Query("SELECT * FROM test WHERE classId = :classId AND lessonId = :lessonId")
    suspend fun getTestsByClassAndLesson(classId: String, lessonId: String): List<TestEntity>
}