package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.TestEntity

@Dao
interface TestDao : BaseDao<TestEntity> {
    @Query("SELECT * FROM test WHERE classId = :classId AND lessonId = :lessonId")
    suspend fun getTestsByClassAndLesson(classId: String, lessonId: String): List<TestEntity>

    @Query("SELECT * FROM test WHERE lessonId = :lessonId")
    suspend fun getTestsByLesson(lessonId: String): List<TestEntity>

    @Query("SELECT * FROM test WHERE id = :id")
    suspend fun getById(id: String): TestEntity?

    @Query("DELETE FROM test WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM test WHERE title LIKE '%' || :query || '%' AND (:lessonId IS NULL OR lessonId = :lessonId)")
    suspend fun searchByTitle(query: String, lessonId: String? = null): List<TestEntity>
}