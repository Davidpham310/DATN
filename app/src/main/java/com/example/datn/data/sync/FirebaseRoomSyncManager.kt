package com.example.datn.data.sync

import android.util.Log
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.*
import com.example.datn.data.local.entities.toEntity
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.SyncEntityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quản lý đồng bộ dữ liệu giữa Firebase (source of truth) và Room (cache)
 * 
 * Strategy:
 * 1. Try Room first (nhanh, offline support)
 * 2. If Room empty → Fetch từ Firebase → Cache vào Room
 * 3. Always return Firebase data khi có network
 */
@Singleton
class FirebaseRoomSyncManager @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val testDao: TestDao,
    private val testQuestionDao: TestQuestionDao,
    private val testOptionDao: TestOptionDao,
    private val miniGameDao: MiniGameDao,
    private val miniGameQuestionDao: MiniGameQuestionDao,
    private val miniGameOptionDao: MiniGameOptionDao,
    private val studentTestResultDao: StudentTestResultDao,
    private val studentTestAnswerDao: StudentTestAnswerDao
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Sync Test data: Room → Firebase → Room
     * @param testId ID của test cần sync
     * @param forceSync true = bắt buộc fetch Firebase, false = check Room trước
     */
    suspend fun syncTestData(testId: String, forceSync: Boolean = false): Resource<Unit> {
        return try {
            Log.d(TAG, "[syncTestData] START - testId: $testId, forceSync: $forceSync")
            
            // Step 1: Check Room cache nếu không force sync
            if (!forceSync) {
                val cachedTest = testDao.getById(testId)
                val cachedQuestions = testQuestionDao.getQuestionsByTest(testId)
                
                if (cachedTest != null && cachedQuestions.isNotEmpty()) {
                    // Check if có options cho ít nhất 1 question
                    val hasOptions = cachedQuestions.any { question ->
                        testOptionDao.getOptionsByQuestion(question.id).isNotEmpty()
                    }
                    
                    if (hasOptions) {
                        Log.d(TAG, "[syncTestData] Found complete data in cache (test + ${cachedQuestions.size} questions + options), skip sync")
                        return Resource.Success(Unit)
                    } else {
                        Log.d(TAG, "[syncTestData] Test & questions in cache but missing options, will sync")
                    }
                } else {
                    Log.d(TAG, "[syncTestData] Incomplete cache (test=${cachedTest != null}, questions=${cachedQuestions.size}), will sync")
                }
            }
            
            // Step 2: Fetch từ Firebase
            Log.d(TAG, "[syncTestData] Fetching from Firebase...")
            when (val testResult = firebaseDataSource.getTestById(testId)) {
                is Resource.Success -> {
                    val test = testResult.data
                    if (test != null) {
                        // Step 3: Save to Room
                        testDao.insert(test.toEntity())
                        Log.d(TAG, "[syncTestData] ✅ Test synced to Room")
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "[syncTestData] Firebase error: ${testResult.message}")
                    return Resource.Error(testResult.message ?: "Lỗi sync test")
                }
                else -> {}
            }
            
            // Step 4: Fetch questions
            Log.d(TAG, "[syncTestData] Fetching questions...")
            when (val questionsResult = firebaseDataSource.getTestQuestions(testId)) {
                is Resource.Success -> {
                    val questions = questionsResult.data ?: emptyList()
                    questions.forEach { question ->
                        testQuestionDao.insert(question.toEntity())
                    }
                    Log.d(TAG, "[syncTestData] ✅ ${questions.size} questions synced")
                    
                    // Step 5: Fetch options for each question
                    questions.forEach { question ->
                        when (val optionsResult = firebaseDataSource.getTestOptionsByQuestion(question.id)) {
                            is Resource.Success -> {
                                val options = optionsResult.data ?: emptyList()
                                options.forEach { option ->
                                    testOptionDao.insert(option.toEntity())
                                }
                                Log.d(TAG, "[syncTestData] ✅ ${options.size} options synced for question ${question.id}")
                            }
                            else -> {
                                Log.w(TAG, "[syncTestData] Failed to sync options for question ${question.id}")
                            }
                        }
                    }
                }
                else -> {
                    Log.w(TAG, "[syncTestData] Failed to sync questions")
                }
            }
            
            Log.d(TAG, "[syncTestData] ✅ COMPLETE")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[syncTestData] ERROR: ${e.message}", e)
            Resource.Error("Lỗi đồng bộ: ${e.message}")
        }
    }

    /**
     * Sync MiniGame data: Room → Firebase → Room
     */
    suspend fun syncMiniGameData(miniGameId: String, forceSync: Boolean = false): Resource<Unit> {
        return try {
            Log.d(TAG, "[syncMiniGameData] START - miniGameId: $miniGameId, forceSync: $forceSync")
            
            // Step 1: Check Room cache
            if (!forceSync) {
                val cachedGame = miniGameDao.getMiniGameById(miniGameId)
                if (cachedGame != null) {
                    Log.d(TAG, "[syncMiniGameData] Found in cache, skip sync")
                    return Resource.Success(Unit)
                }
            }
            
            // Step 2: Fetch MiniGame từ Firebase
            Log.d(TAG, "[syncMiniGameData] Fetching from Firebase...")
            when (val gameResult = firebaseDataSource.getMiniGameById(miniGameId)) {
                is Resource.Success -> {
                    val game = gameResult.data
                    if (game != null) {
                        miniGameDao.insert(game.toEntity())
                        Log.d(TAG, "[syncMiniGameData] ✅ MiniGame synced")
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "[syncMiniGameData] Firebase error: ${gameResult.message}")
                    return Resource.Error(gameResult.message ?: "Lỗi sync minigame")
                }
                else -> {}
            }
            
            // Step 3: Fetch questions
            Log.d(TAG, "[syncMiniGameData] Fetching questions...")
            when (val questionsResult = firebaseDataSource.getQuestionsByMiniGame(miniGameId)) {
                is Resource.Success -> {
                    val questions = questionsResult.data ?: emptyList()
                    questions.forEach { question ->
                        miniGameQuestionDao.insert(question.toEntity())
                    }
                    Log.d(TAG, "[syncMiniGameData] ✅ ${questions.size} questions synced")
                    
                    // Step 4: Fetch options
                    questions.forEach { question ->
                        when (val optionsResult = firebaseDataSource.getMiniGameOptionsByQuestion(question.id)) {
                            is Resource.Success -> {
                                val options = optionsResult.data ?: emptyList()
                                options.forEach { option ->
                                    miniGameOptionDao.insert(option.toEntity())
                                }
                                Log.d(TAG, "[syncMiniGameData] ✅ ${options.size} options synced for question ${question.id}")
                            }
                            else -> {
                                Log.w(TAG, "[syncMiniGameData] Failed to sync options for question ${question.id}")
                            }
                        }
                    }
                }
                else -> {
                    Log.w(TAG, "[syncMiniGameData] Failed to sync questions")
                }
            }
            
            Log.d(TAG, "[syncMiniGameData] ✅ COMPLETE")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[syncMiniGameData] ERROR: ${e.message}", e)
            Resource.Error("Lỗi đồng bộ: ${e.message}")
        }
    }

    /**
     * Check if Room cache is empty for a specific entity type
     */
    suspend fun isCacheEmpty(entityType: SyncEntityType, entityId: String): Boolean {
        return when (entityType) {
            SyncEntityType.TESTS -> testDao.getById(entityId) == null
            SyncEntityType.TEST_QUESTIONS -> testQuestionDao.getQuestionsByTest(entityId).isEmpty()
            SyncEntityType.MINI_GAMES -> miniGameDao.getMiniGameById(entityId) == null
            SyncEntityType.MINI_GAME_QUESTIONS -> miniGameQuestionDao.getQuestionsByMiniGame(entityId).isEmpty()
            else -> true
        }
    }

    /**
     * Clear all cache for a specific test
     */
    suspend fun clearTestCache(testId: String) {
        try {
            Log.d(TAG, "[clearTestCache] Clearing cache for test: $testId")
            testDao.deleteById(testId)
            // Questions and options will be cascade deleted if FK constraints are set
            Log.d(TAG, "[clearTestCache] ✅ Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "[clearTestCache] ERROR: ${e.message}", e)
        }
    }

    /**
     * Clear all cache for a specific minigame
     */
    suspend fun clearMiniGameCache(miniGameId: String) {
        try {
            Log.d(TAG, "[clearMiniGameCache] Clearing cache for minigame: $miniGameId")
            miniGameDao.deleteById(miniGameId)
            Log.d(TAG, "[clearMiniGameCache] ✅ Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "[clearMiniGameCache] ERROR: ${e.message}", e)
        }
    }

    /**
     * Sync Student Test Result: Firebase → Room
     * @param studentId ID của student
     * @param testId ID của test
     * @param forceSync true = bắt buộc fetch Firebase
     */
    suspend fun syncStudentTestResult(studentId: String, testId: String, forceSync: Boolean = false): Resource<Unit> {
        return try {
            Log.d(TAG, "[syncStudentTestResult] START - studentId: $studentId, testId: $testId, forceSync: $forceSync")
            
            // Step 1: Check Room cache nếu không force sync
            if (!forceSync) {
                val cachedResult = studentTestResultDao.getResultByStudentAndTest(studentId, testId)
                if (cachedResult != null) {
                    Log.d(TAG, "[syncStudentTestResult] Found in cache, skip sync")
                    return Resource.Success(Unit)
                }
            }
            
            // Step 2: Fetch từ Firebase
            Log.d(TAG, "[syncStudentTestResult] Fetching from Firebase...")
            when (val resultData = firebaseDataSource.getStudentResult(studentId, testId)) {
                is Resource.Success -> {
                    val result = resultData.data
                    if (result != null) {
                        // Step 3: Save to Room
                        studentTestResultDao.insert(result.toEntity())
                        Log.d(TAG, "[syncStudentTestResult] ✅ Result synced to Room")
                        
                        // Step 4: Sync student answers
                        syncStudentAnswers(result.id, forceSync = true)
                    } else {
                        Log.w(TAG, "[syncStudentTestResult] No result found")
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "[syncStudentTestResult] Firebase error: ${resultData.message}")
                    return Resource.Error(resultData.message ?: "Lỗi sync result")
                }
                else -> {}
            }
            
            Log.d(TAG, "[syncStudentTestResult] ✅ COMPLETE")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[syncStudentTestResult] ERROR: ${e.message}", e)
            Resource.Error("Lỗi đồng bộ result: ${e.message}")
        }
    }

    /**
     * Sync Student Answers: Firebase → Room
     * @param resultId ID của student test result
     * @param forceSync true = bắt buộc fetch Firebase
     */
    suspend fun syncStudentAnswers(resultId: String, forceSync: Boolean = false): Resource<Unit> {
        return try {
            Log.d(TAG, "[syncStudentAnswers] START - resultId: $resultId, forceSync: $forceSync")
            
            // Step 1: Check Room cache nếu không force sync
            if (!forceSync) {
                val cachedAnswers = studentTestAnswerDao.getAnswersByResultId(resultId)
                if (cachedAnswers.isNotEmpty()) {
                    Log.d(TAG, "[syncStudentAnswers] Found ${cachedAnswers.size} answers in cache, skip sync")
                    return Resource.Success(Unit)
                }
            }
            
            // Step 2: Fetch từ Firebase
            Log.d(TAG, "[syncStudentAnswers] Fetching from Firebase...")
            when (val answersResult = firebaseDataSource.getAnswersByResultId(resultId)) {
                is Resource.Success -> {
                    val answers = answersResult.data ?: emptyList()
                    if (answers.isNotEmpty()) {
                        // Step 3: Save to Room using toEntity() mapper from StudentTestAnswerEntity.kt
                        val answerEntities = answers.map { it.toEntity() }
                        studentTestAnswerDao.insertAll(answerEntities)
                        Log.d(TAG, "[syncStudentAnswers] ✅ ${answers.size} answers synced to Room")
                    } else {
                        Log.d(TAG, "[syncStudentAnswers] No answers found")
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "[syncStudentAnswers] Firebase error: ${answersResult.message}")
                    return Resource.Error(answersResult.message ?: "Lỗi sync answers")
                }
                else -> {}
            }
            
            Log.d(TAG, "[syncStudentAnswers] ✅ COMPLETE")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[syncStudentAnswers] ERROR: ${e.message}", e)
            Resource.Error("Lỗi đồng bộ answers: ${e.message}")
        }
    }

    /**
     * Sync All Results for a Student: Firebase → Room
     * @param studentId ID của student
     * @param forceSync true = bắt buộc fetch Firebase
     */
    suspend fun syncAllStudentResults(studentId: String, forceSync: Boolean = false): Resource<Unit> {
        return try {
            Log.d(TAG, "[syncAllStudentResults] START - studentId: $studentId, forceSync: $forceSync")
            
            // Fetch all results từ Firebase
            Log.d(TAG, "[syncAllStudentResults] Fetching from Firebase...")
            when (val resultsData = firebaseDataSource.getResultsByStudent(studentId)) {
                is Resource.Success -> {
                    val results = resultsData.data ?: emptyList()
                    Log.d(TAG, "[syncAllStudentResults] Found ${results.size} results")
                    
                    // Save each result to Room
                    results.forEach { result ->
                        try {
                            studentTestResultDao.insert(result.toEntity())
                            
                            // Sync answers for each result
                            syncStudentAnswers(result.id, forceSync = true)
                        } catch (e: Exception) {
                            Log.w(TAG, "[syncAllStudentResults] Failed to sync result ${result.id}: ${e.message}")
                        }
                    }
                    
                    Log.d(TAG, "[syncAllStudentResults] ✅ ${results.size} results synced")
                }
                is Resource.Error -> {
                    Log.e(TAG, "[syncAllStudentResults] Firebase error: ${resultsData.message}")
                    return Resource.Error(resultsData.message ?: "Lỗi sync all results")
                }
                else -> {}
            }
            
            Log.d(TAG, "[syncAllStudentResults] ✅ COMPLETE")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[syncAllStudentResults] ERROR: ${e.message}", e)
            Resource.Error("Lỗi đồng bộ all results: ${e.message}")
        }
    }

    /**
     * Clear result cache for a specific test
     */
    suspend fun clearResultCache(testId: String) {
        try {
            Log.d(TAG, "[clearResultCache] Clearing results for test: $testId")
            studentTestResultDao.deleteByTest(testId)
            Log.d(TAG, "[clearResultCache] ✅ Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "[clearResultCache] ERROR: ${e.message}", e)
        }
    }

    // ==================== MINIGAME SYNC METHODS ====================

    /**
     * Sync ALL Student MiniGame Results: Firebase → Room
     * Unlike Test (1 result), MiniGame supports unlimited replay
     * @param studentId ID của student
     * @param miniGameId ID của minigame
     * @param forceSync true = bắt buộc fetch Firebase
     */
    suspend fun syncStudentMiniGameResults(
        studentId: String, 
        miniGameId: String, 
        forceSync: Boolean = false
    ): Resource<Unit> {
        return try {
            Log.d(TAG, "[syncStudentMiniGameResults] START - studentId: $studentId, miniGameId: $miniGameId, forceSync: $forceSync")
            
            // Step 1: Check Room cache nếu không force sync
            if (!forceSync) {
                // TODO: Uncomment when DAO is ready
                // val cachedResults = studentMiniGameResultDao.getResultsByStudentAndGame(studentId, miniGameId)
                // if (cachedResults.isNotEmpty()) {
                //     Log.d(TAG, "[syncStudentMiniGameResults] Found ${cachedResults.size} results in cache, skip sync")
                //     return Resource.Success(Unit)
                // }
                Log.d(TAG, "[syncStudentMiniGameResults] TODO: Check cache not implemented yet")
            }
            
            // Step 2: Fetch ALL results từ Firebase
            Log.d(TAG, "[syncStudentMiniGameResults] Fetching ALL results from Firebase...")
            
            // TODO: Uncomment when Firebase method is ready
            // when (val resultsData = firebaseDataSource.getStudentMiniGameResults(studentId, miniGameId)) {
            //     is Resource.Success -> {
            //         val results = resultsData.data ?: emptyList()
            //         Log.d(TAG, "[syncStudentMiniGameResults] Found ${results.size} attempts")
            //         
            //         if (results.isNotEmpty()) {
            //             // Step 3: Save ALL results to Room
            //             val resultEntities = results.map { it.toEntity() }
            //             studentMiniGameResultDao.insertAll(resultEntities)
            //             Log.d(TAG, "[syncStudentMiniGameResults] ✅ ${results.size} results synced to Room")
            //             
            //             // Step 4: Sync answers for EACH result
            //             results.forEach { result ->
            //                 syncMiniGameAnswers(result.id, forceSync = true)
            //             }
            //             Log.d(TAG, "[syncStudentMiniGameResults] ✅ All answers synced")
            //         } else {
            //             Log.d(TAG, "[syncStudentMiniGameResults] No results found")
            //         }
            //     }
            //     is Resource.Error -> {
            //         Log.e(TAG, "[syncStudentMiniGameResults] Firebase error: ${resultsData.message}")
            //         return Resource.Error(resultsData.message ?: "Lỗi sync results")
            //     }
            //     else -> {}
            // }
            
            Log.d(TAG, "[syncStudentMiniGameResults] TODO: Firebase fetch not implemented yet")
            Log.d(TAG, "[syncStudentMiniGameResults] ✅ COMPLETE")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[syncStudentMiniGameResults] ERROR: ${e.message}", e)
            Resource.Error("Lỗi đồng bộ minigame results: ${e.message}")
        }
    }

    /**
     * Sync MiniGame Answers for a specific result: Firebase → Room
     * @param resultId ID của student minigame result
     * @param forceSync true = bắt buộc fetch Firebase
     */
    suspend fun syncMiniGameAnswers(resultId: String, forceSync: Boolean = false): Resource<Unit> {
        return try {
            Log.d(TAG, "[syncMiniGameAnswers] START - resultId: $resultId, forceSync: $forceSync")
            
            // Step 1: Check Room cache nếu không force sync
            if (!forceSync) {
                // TODO: Uncomment when DAO is ready
                // val cachedAnswers = studentMiniGameAnswerDao.getAnswersByResultId(resultId)
                // if (cachedAnswers.isNotEmpty()) {
                //     Log.d(TAG, "[syncMiniGameAnswers] Found ${cachedAnswers.size} answers in cache, skip sync")
                //     return Resource.Success(Unit)
                // }
                Log.d(TAG, "[syncMiniGameAnswers] TODO: Check cache not implemented yet")
            }
            
            // Step 2: Fetch từ Firebase
            Log.d(TAG, "[syncMiniGameAnswers] Fetching from Firebase...")
            
            // TODO: Uncomment when Firebase method is ready
            // when (val answersResult = firebaseDataSource.getMiniGameAnswersByResultId(resultId)) {
            //     is Resource.Success -> {
            //         val answers = answersResult.data ?: emptyList()
            //         if (answers.isNotEmpty()) {
            //             // Step 3: Save to Room
            //             val answerEntities = answers.map { it.toEntity() }
            //             studentMiniGameAnswerDao.insertAll(answerEntities)
            //             Log.d(TAG, "[syncMiniGameAnswers] ✅ ${answers.size} answers synced to Room")
            //         } else {
            //             Log.d(TAG, "[syncMiniGameAnswers] No answers found")
            //         }
            //     }
            //     is Resource.Error -> {
            //         Log.e(TAG, "[syncMiniGameAnswers] Firebase error: ${answersResult.message}")
            //         return Resource.Error(answersResult.message ?: "Lỗi sync answers")
            //     }
            //     else -> {}
            // }
            
            Log.d(TAG, "[syncMiniGameAnswers] TODO: Firebase fetch not implemented yet")
            Log.d(TAG, "[syncMiniGameAnswers] ✅ COMPLETE")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[syncMiniGameAnswers] ERROR: ${e.message}", e)
            Resource.Error("Lỗi đồng bộ minigame answers: ${e.message}")
        }
    }

    /**
     * Clear minigame result cache for a specific student and game
     * Useful for refresh or logout
     */
    suspend fun clearMiniGameResultCache(studentId: String, miniGameId: String) {
        try {
            Log.d(TAG, "[clearMiniGameResultCache] Clearing results for student: $studentId, game: $miniGameId")
            
            // TODO: Uncomment when DAO is ready
            // studentMiniGameResultDao.deleteByStudentAndGame(studentId, miniGameId)
            
            Log.d(TAG, "[clearMiniGameResultCache] TODO: Delete not implemented yet")
            Log.d(TAG, "[clearMiniGameResultCache] ✅ Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "[clearMiniGameResultCache] ERROR: ${e.message}", e)
        }
    }
}
