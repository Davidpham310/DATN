package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.StudentTestAnswerEntity

@Dao
interface StudentTestAnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: StudentTestAnswerEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(answers: List<StudentTestAnswerEntity>)
    
    @Query("SELECT * FROM student_test_answers WHERE resultId = :resultId")
    suspend fun getAnswersByResultId(resultId: String): List<StudentTestAnswerEntity>
    
    @Query("SELECT * FROM student_test_answers WHERE questionId = :questionId LIMIT 1")
    suspend fun getAnswerByQuestionId(questionId: String): StudentTestAnswerEntity?
    
    @Query("DELETE FROM student_test_answers WHERE resultId = :resultId")
    suspend fun deleteByResultId(resultId: String)
    
    @Query("DELETE FROM student_test_answers")
    suspend fun deleteAll()
}
