package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.StudentMiniGameResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentMiniGameResultDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: StudentMiniGameResultEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<StudentMiniGameResultEntity>)
    
    @Update
    suspend fun update(result: StudentMiniGameResultEntity)
    
    @Delete
    suspend fun delete(result: StudentMiniGameResultEntity)
    
    /**
     * Get ALL results for a student and game (for unlimited replay)
     */
    @Query("SELECT * FROM student_minigame_result WHERE studentId = :studentId AND miniGameId = :miniGameId ORDER BY attemptNumber DESC")
    suspend fun getResultsByStudentAndGame(studentId: String, miniGameId: String): List<StudentMiniGameResultEntity>
    
    /**
     * Get ALL results for a student and game as Flow (for live updates)
     */
    @Query("SELECT * FROM student_minigame_result WHERE studentId = :studentId AND miniGameId = :miniGameId ORDER BY attemptNumber DESC")
    fun getResultsByStudentAndGameFlow(studentId: String, miniGameId: String): Flow<List<StudentMiniGameResultEntity>>
    
    /**
     * Get a specific result by ID
     */
    @Query("SELECT * FROM student_minigame_result WHERE id = :resultId")
    suspend fun getResultById(resultId: String): StudentMiniGameResultEntity?
    
    /**
     * Get a specific result by ID as Flow
     */
    @Query("SELECT * FROM student_minigame_result WHERE id = :resultId")
    fun getResultByIdFlow(resultId: String): Flow<StudentMiniGameResultEntity?>
    
    /**
     * Get latest result for a student and game
     */
    @Query("SELECT * FROM student_minigame_result WHERE studentId = :studentId AND miniGameId = :miniGameId ORDER BY attemptNumber DESC LIMIT 1")
    suspend fun getLatestResult(studentId: String, miniGameId: String): StudentMiniGameResultEntity?
    
    /**
     * Get best result (highest score) for a student and game
     */
    @Query("SELECT * FROM student_minigame_result WHERE studentId = :studentId AND miniGameId = :miniGameId ORDER BY score DESC LIMIT 1")
    suspend fun getBestResult(studentId: String, miniGameId: String): StudentMiniGameResultEntity?
    
    /**
     * Get all results for a student (all games)
     */
    @Query("SELECT * FROM student_minigame_result WHERE studentId = :studentId ORDER BY submissionTime DESC")
    suspend fun getAllResultsByStudent(studentId: String): List<StudentMiniGameResultEntity>
    
    /**
     * Get all results for a game (all students)
     */
    @Query("SELECT * FROM student_minigame_result WHERE miniGameId = :miniGameId ORDER BY submissionTime DESC")
    suspend fun getAllResultsByGame(miniGameId: String): List<StudentMiniGameResultEntity>
    
    /**
     * Delete all results for a student and game
     */
    @Query("DELETE FROM student_minigame_result WHERE studentId = :studentId AND miniGameId = :miniGameId")
    suspend fun deleteByStudentAndGame(studentId: String, miniGameId: String)
    
    /**
     * Delete all results for a game
     */
    @Query("DELETE FROM student_minigame_result WHERE miniGameId = :miniGameId")
    suspend fun deleteByGame(miniGameId: String)
    
    /**
     * Delete all results
     */
    @Query("DELETE FROM student_minigame_result")
    suspend fun deleteAll()
    
    /**
     * Get attempt count for a student and game
     */
    @Query("SELECT COUNT(*) FROM student_minigame_result WHERE studentId = :studentId AND miniGameId = :miniGameId")
    suspend fun getAttemptCount(studentId: String, miniGameId: String): Int
}
