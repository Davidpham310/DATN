package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentMiniGameAnswer
import kotlinx.coroutines.flow.Flow

interface IMiniGameRepository {
    // ==================== GAME OPERATIONS ====================
    fun createGame(game: MiniGame): Flow<Resource<MiniGame>>
    fun getGameById(gameId: String): Flow<Resource<MiniGame?>>
    fun getQuestionsByGame(gameId: String): Flow<Resource<List<MiniGameQuestion>>>
    fun getFilteredGames(type: String?, level: String?): Flow<Resource<List<MiniGame>>>
    fun getGamesByTeacher(teacherId: String): Flow<Resource<List<MiniGame>>>
    fun getGamesByLesson(lessonId: String): Flow<Resource<List<MiniGame>>>
    fun updateGame(game: MiniGame): Flow<Resource<MiniGame>>
    fun deleteGame(gameId: String): Flow<Resource<Unit>>
    fun searchGames(query: String, teacherId: String? = null): Flow<Resource<List<MiniGame>>>

    // ==================== QUESTION OPERATIONS ====================
    fun createQuestion(question: MiniGameQuestion): Flow<Resource<MiniGameQuestion>>
    fun updateQuestion(question: MiniGameQuestion): Flow<Resource<MiniGameQuestion>>
    fun deleteQuestion(questionId: String): Flow<Resource<Unit>>
    fun getQuestionById(questionId: String): Flow<Resource<MiniGameQuestion?>>

    // ==================== OPTION OPERATIONS ====================
    fun createOption(option: MiniGameOption): Flow<Resource<MiniGameOption>>
    fun updateOption(option: MiniGameOption): Flow<Resource<MiniGameOption>>
    fun deleteOption(optionId: String): Flow<Resource<Unit>>
    fun getOptionsByQuestion(questionId: String): Flow<Resource<List<MiniGameOption>>>
    fun getOptionById(optionId: String): Flow<Resource<MiniGameOption?>>

    // ==================== RESULT OPERATIONS (Unlimited Replay) ====================
    /**
     * Submit a mini game result with answers
     * Automatically increments attempt number
     */
    fun submitMiniGameResult(
        result: StudentMiniGameResult,
        answers: List<StudentMiniGameAnswer>
    ): Flow<Resource<StudentMiniGameResult>>

    /**
     * Get ALL results for a student and game (for unlimited replay)
     */
    fun getAllStudentResults(
        studentId: String,
        miniGameId: String
    ): Flow<Resource<List<StudentMiniGameResult>>>

    fun getAllResultsByStudent(
        studentId: String
    ): Flow<Resource<List<StudentMiniGameResult>>>

    /**
     * Get a specific result by ID
     */
    fun getStudentResult(
        resultId: String
    ): Flow<Resource<StudentMiniGameResult?>>

    /**
     * Get the latest result for a student and game
     */
    fun getLatestResult(
        studentId: String,
        miniGameId: String
    ): Flow<Resource<StudentMiniGameResult?>>

    /**
     * Get the best result (highest score) for a student and game
     */
    fun getBestResult(
        studentId: String,
        miniGameId: String
    ): Flow<Resource<StudentMiniGameResult?>>

    // ==================== ANSWER OPERATIONS ====================
    /**
     * Get all answers for a specific result
     */
    fun getStudentAnswers(
        resultId: String
    ): Flow<Resource<List<StudentMiniGameAnswer>>>
}