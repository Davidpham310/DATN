package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.TestQuestionEntity

@Dao
interface TestQuestionDao : BaseDao<TestQuestionEntity> {
    @Query("SELECT * FROM test_question WHERE testId = :testId ORDER BY 'order'")
    suspend fun getQuestionsByTest(testId: String): List<TestQuestionEntity>
}