package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.QuestionEntity

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(q: QuestionEntity)

    @Query("SELECT * FROM questions WHERE testId = :testId")
    suspend fun getQuestionsForTest(testId: String): List<QuestionEntity>

    @Delete
    suspend fun deleteQuestion(q: QuestionEntity)
}
