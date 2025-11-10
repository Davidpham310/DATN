package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.StudentMiniGameAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentMiniGameAnswerDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: StudentMiniGameAnswerEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(answers: List<StudentMiniGameAnswerEntity>)
    
    @Update
    suspend fun update(answer: StudentMiniGameAnswerEntity)
    
    @Delete
    suspend fun delete(answer: StudentMiniGameAnswerEntity)
    
    /**
     * Get all answers for a specific result
     */
    @Query("SELECT * FROM student_minigame_answer WHERE resultId = :resultId ORDER BY createdAt ASC")
    suspend fun getAnswersByResultId(resultId: String): List<StudentMiniGameAnswerEntity>
    
    /**
     * Get all answers for a specific result as Flow
     */
    @Query("SELECT * FROM student_minigame_answer WHERE resultId = :resultId ORDER BY createdAt ASC")
    fun getAnswersByResultIdFlow(resultId: String): Flow<List<StudentMiniGameAnswerEntity>>
    
    /**
     * Get answer by question ID and result ID
     */
    @Query("SELECT * FROM student_minigame_answer WHERE resultId = :resultId AND questionId = :questionId")
    suspend fun getAnswerByResultAndQuestion(resultId: String, questionId: String): StudentMiniGameAnswerEntity?
    
    /**
     * Get answer by ID
     */
    @Query("SELECT * FROM student_minigame_answer WHERE id = :answerId")
    suspend fun getAnswerById(answerId: String): StudentMiniGameAnswerEntity?
    
    /**
     * Delete all answers for a result
     */
    @Query("DELETE FROM student_minigame_answer WHERE resultId = :resultId")
    suspend fun deleteByResultId(resultId: String)
    
    /**
     * Delete all answers
     */
    @Query("DELETE FROM student_minigame_answer")
    suspend fun deleteAll()
    
    /**
     * Get correct answers count for a result
     */
    @Query("SELECT COUNT(*) FROM student_minigame_answer WHERE resultId = :resultId AND isCorrect = 1")
    suspend fun getCorrectAnswersCount(resultId: String): Int
    
    /**
     * Get total answers count for a result
     */
    @Query("SELECT COUNT(*) FROM student_minigame_answer WHERE resultId = :resultId")
    suspend fun getTotalAnswersCount(resultId: String): Int
}
