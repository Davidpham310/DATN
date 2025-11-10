package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.StudentTestAnswerDao
import com.example.datn.data.local.dao.StudentTestResultDao
import com.example.datn.data.local.dao.TestDao
import com.example.datn.data.local.dao.TestOptionDao
import com.example.datn.data.local.dao.TestQuestionDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.data.sync.FirebaseRoomSyncManager
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.StudentTestAnswer
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.models.TestStatus
import com.example.datn.data.local.entities.toEntity
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val testDao: TestDao,
    private val testQuestionDao: TestQuestionDao,
    private val studentTestResultDao: StudentTestResultDao,
    private val testOptionDao: TestOptionDao,
    private val studentTestAnswerDao: StudentTestAnswerDao,
    private val syncManager: FirebaseRoomSyncManager
) : ITestRepository {

    override fun createTest(test: Test): Flow<Resource<Test>> = flow {
        try {
            emit(Resource.Loading())
            val withId = if (test.id.isBlank()) test.copy(id = UUID.randomUUID().toString()) else test
            val withTimestamps = withId.copy(
                createdAt = if (withId.createdAt.toEpochMilli() == 0L) Instant.now() else withId.createdAt,
                updatedAt = Instant.now()
            )
            when (val result = firebaseDataSource.addTest(withTimestamps)) {
                is Resource.Success -> {
                    val saved = result.data ?: withTimestamps
                    testDao.insert(saved.toEntity())
                    emit(Resource.Success(saved))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tạo bài kiểm tra: ${e.message}"))
        }
    }

    override fun updateTest(test: Test): Flow<Resource<Test>> = flow {
        try {
            emit(Resource.Loading())
            val withTimestamp = test.copy(updatedAt = Instant.now())
            when (val result = firebaseDataSource.updateTest(withTimestamp)) {
                is Resource.Success -> {
                    val updated = result.data ?: withTimestamp
                    testDao.insert(updated.toEntity())
                    emit(Resource.Success(updated))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi cập nhật bài kiểm tra: ${e.message}"))
        }
    }

    override fun deleteTest(testId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.deleteTest(testId)) {
                is Resource.Success -> {
                    testDao.deleteById(testId)
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi xóa bài kiểm tra: ${e.message}"))
        }
    }

    override fun getTestDetails(testId: String): Flow<Resource<Test>> = flow {
        try {
            android.util.Log.d("TestRepo", ">>> getTestDetails START - testId: $testId")
            emit(Resource.Loading())
            
            // ========== STEP 1: SYNC DATA FROM FIREBASE TO ROOM ==========
            // Check if cache exists, if not -> sync from Firebase
            android.util.Log.d("TestRepo", ">>> Calling syncManager.syncTestData()")
            syncManager.syncTestData(testId, forceSync = false)
            android.util.Log.d("TestRepo", ">>> syncTestData COMPLETE")
            
            // ========== STEP 2: ALWAYS FETCH FROM FIREBASE (SOURCE OF TRUTH) ==========
            when (val result = firebaseDataSource.getTestById(testId)) {
                is Resource.Success -> {
                    val remote = result.data
                    if (remote != null) {
                        // Cache to Room for offline support
                        testDao.insert(remote.toEntity())
                        emit(Resource.Success(remote))
                    } else {
                        // Fallback to Room cache if Firebase returns null
                        val cached = testDao.getById(testId)
                        if (cached != null) {
                            emit(Resource.Success(cached.toDomain()))
                        } else {
                            emit(Resource.Error("Không tìm thấy bài kiểm tra"))
                        }
                    }
                }
                is Resource.Error -> {
                    // Network error -> Fallback to Room cache
                    val cached = testDao.getById(testId)
                    if (cached != null) {
                        emit(Resource.Success(cached.toDomain()))
                    } else {
                        emit(Resource.Error(result.message))
                    }
                }
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy thông tin bài kiểm tra: ${e.message}"))
        }
    }

    override fun getTestsByLesson(lessonId: String): Flow<Resource<List<Test>>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getTestsByLesson(lessonId)) {
                is Resource.Success -> {
                    val tests = result.data ?: emptyList()
                    tests.forEach { testDao.insert(it.toEntity()) }
                    emit(Resource.Success(tests))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách bài kiểm tra: ${e.message}"))
        }
    }

    override fun getTestsByClasses(classIds: List<String>): Flow<Resource<List<Test>>> = flow {
        try {
            android.util.Log.d("TestRepository", ">>> getTestsByClasses START - classIds: $classIds")
            emit(Resource.Loading())
            
            if (classIds.isEmpty()) {
                android.util.Log.w("TestRepository", ">>> Empty classIds, returning empty list")
                emit(Resource.Success(emptyList()))
                return@flow
            }
            
            // Fetch from Firebase for each classId
            val allTests = mutableListOf<Test>()
            
            for (classId in classIds) {
                android.util.Log.d("TestRepository", ">>> Fetching tests for classId: $classId")
                
                when (val result = firebaseDataSource.getTestsByClassId(classId)) {
                    is Resource.Success -> {
                        val tests = result.data ?: emptyList()
                        android.util.Log.d("TestRepository", ">>> Firebase returned ${tests.size} tests for class $classId")
                        
                        // Cache to Room
                        tests.forEach { test ->
                            try {
                                testDao.insert(test.toEntity())
                            } catch (e: Exception) {
                                android.util.Log.w("TestRepository", "Failed to cache test ${test.id}: ${e.message}")
                            }
                        }
                        
                        allTests.addAll(tests)
                    }
                    is Resource.Error -> {
                        android.util.Log.e("TestRepository", ">>> Firebase error for class $classId: ${result.message}")
                        // Fallback to Room cache
                        val cachedTests = testDao.getTestsByClasses(listOf(classId)).map { it.toDomain() }
                        android.util.Log.d("TestRepository", ">>> Fallback: Found ${cachedTests.size} cached tests")
                        allTests.addAll(cachedTests)
                    }
                    else -> {}
                }
            }
            
            // Remove duplicates and emit
            val uniqueTests = allTests.distinctBy { it.id }
            android.util.Log.d("TestRepository", ">>> Returning ${uniqueTests.size} unique tests")
            emit(Resource.Success(uniqueTests))
            
        } catch (e: Exception) {
            android.util.Log.e("TestRepository", ">>> Fatal error: ${e.message}", e)
            emit(Resource.Error("Lỗi khi lấy danh sách bài kiểm tra: ${e.message}"))
        }
    }

    override fun submitTest(
        studentId: String,
        testId: String,
        answers: Map<String, List<String>>
    ): Flow<Resource<StudentTestResult>> = flow {
        try {
            emit(Resource.Loading())
            // Scoring is domain-specific; here we just persist a result shell. Customize as needed.
            val now = Instant.now()
            val result = StudentTestResult(
                id = UUID.randomUUID().toString(),
                studentId = studentId,
                testId = testId,
                score = 0.0,
                completionStatus = TestStatus.COMPLETED,
                submissionTime = now,
                durationSeconds = 0,
                createdAt = now,
                updatedAt = now
            )
            when (val remote = firebaseDataSource.submitTestResult(result)) {
                is Resource.Success -> {
                    val saved = remote.data ?: result
                    studentTestResultDao.insert(saved.toEntity())
                    emit(Resource.Success(saved))
                }
                is Resource.Error -> emit(Resource.Error(remote.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi nộp bài kiểm tra: ${e.message}"))
        }
    }

    override fun submitTestResult(
        result: StudentTestResult,
        answers: Map<String, Any>
    ): Flow<Resource<StudentTestResult>> = flow {
        try {
            emit(Resource.Loading())
            val withTimestamps = result.copy(
                submissionTime = if (result.submissionTime.toEpochMilli() == 0L) Instant.now() else result.submissionTime,
                createdAt = if (result.createdAt.toEpochMilli() == 0L) Instant.now() else result.createdAt,
                updatedAt = Instant.now()
            )
            when (val remote = firebaseDataSource.submitTestResult(withTimestamps)) {
                is Resource.Success -> {
                    val saved = remote.data ?: withTimestamps
                    studentTestResultDao.insert(saved.toEntity())
                    
                    // ========== LƯU ANSWERS VÀO ROOM ==========
                    try {
                        // Get questions để tính điểm từng câu
                        val questions = testQuestionDao.getQuestionsByTest(saved.testId)
                        val studentAnswers = mutableListOf<StudentTestAnswer>()
                        
                        questions.forEach { questionEntity: com.example.datn.data.local.entities.TestQuestionEntity ->
                            val question = questionEntity.toDomain()
                            val answerValue = answers[question.id]
                            
                            if (answerValue != null) {
                                // Get options để grade
                                val options = testOptionDao.getOptionsByQuestion(question.id)
                                    .map { optionEntity: com.example.datn.data.local.entities.TestOptionEntity -> 
                                        optionEntity.toDomain() 
                                    }
                                
                                // Calculate correctness and score
                                val (isCorrect, earnedScore) = gradeAnswer(question, options, answerValue)
                                
                                // Serialize answer
                                val answerString = when (answerValue) {
                                    is String -> answerValue
                                    is List<*> -> answerValue.joinToString(",")
                                    else -> answerValue.toString()
                                }
                                
                                // Create StudentTestAnswer
                                val studentAnswer = StudentTestAnswer(
                                    id = UUID.randomUUID().toString(),
                                    resultId = saved.id,
                                    questionId = question.id,
                                    answer = answerString,
                                    isCorrect = isCorrect,
                                    earnedScore = earnedScore,
                                    createdAt = Instant.now(),
                                    updatedAt = Instant.now()
                                )
                                
                                android.util.Log.d("TestRepository", """
                                    [SAVE ANSWER]
                                    Question: ${question.content}
                                    AnswerString: $answerString
                                    isCorrect: $isCorrect
                                    earnedScore: $earnedScore
                                """.trimIndent())
                                
                                studentAnswers.add(studentAnswer)
                            }
                        }
                        
                        // Save all answers to Room
                        studentTestAnswerDao.insertAll(studentAnswers.map { it.toEntity() })
                        
                        // ========== SYNC TO FIREBASE ==========
                        try {
                            firebaseDataSource.saveStudentAnswers(studentAnswers)
                            android.util.Log.d("TestRepository", "Synced ${studentAnswers.size} answers to Firebase")
                        } catch (e: Exception) {
                            android.util.Log.e("TestRepository", "Error syncing to Firebase: ${e.message}")
                        }
                        // ======================================
                    } catch (e: Exception) {
                        // Log error but don't fail the whole operation
                        android.util.Log.e("TestRepository", "Error saving answers: ${e.message}")
                    }
                    // ==========================================
                    
                    emit(Resource.Success(saved))
                }
                is Resource.Error -> emit(Resource.Error(remote.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi nộp bài kiểm tra: ${e.message}"))
        }
    }
    
    private fun gradeAnswer(
        question: TestQuestion,
        options: List<TestOption>,
        answer: Any
    ): Pair<Boolean, Double> {
        android.util.Log.d("TestRepository", "[gradeAnswer] Grading question: ${question.id}")
        
        return when (question.questionType) {
            QuestionType.SINGLE_CHOICE -> {
                val selectedId = answer as? String ?: return Pair(false, 0.0)
                
                // Find correct option
                val correctOption = options.find { it.isCorrect }
                val correctId = correctOption?.id
                
                // Compare: student answer == correct answer
                val isCorrect = selectedId == correctId && correctId != null
                val earnedScore = if (isCorrect) question.score else 0.0
                
                android.util.Log.d("TestRepository", """
                    [SINGLE_CHOICE]
                    Question: ${question.content}
                    Selected ID: $selectedId
                    Correct ID: $correctId
                    Options: ${options.map { "${it.id.take(8)}... (correct=${it.isCorrect})" }}
                    Match: ${selectedId == correctId}
                    Result: isCorrect=$isCorrect, score=$earnedScore
                """.trimIndent())
                
                Pair(isCorrect, earnedScore)
            }
            QuestionType.MULTIPLE_CHOICE -> {
                val selectedIds = (answer as? List<*>)?.map { it.toString() }?.toSet() ?: return Pair(false, 0.0)
                val correctIds = options.filter { it.isCorrect }.map { it.id }.toSet()
                val isCorrect = selectedIds == correctIds && correctIds.isNotEmpty()
                val earnedScore = if (isCorrect) question.score else 0.0
                
                android.util.Log.d("TestRepository", """
                    [MULTIPLE_CHOICE]
                    Question: ${question.content}
                    Selected IDs: $selectedIds
                    Correct IDs: $correctIds
                    Match: ${selectedIds == correctIds}
                    Result: isCorrect=$isCorrect, score=$earnedScore
                """.trimIndent())
                
                Pair(isCorrect, earnedScore)
            }
            QuestionType.FILL_BLANK -> {
                val studentText = (answer as? String ?: "").trim().lowercase()
                val correctOptions = options.filter { it.isCorrect }
                val isCorrect = correctOptions.any { 
                    it.content.trim().lowercase() == studentText 
                }
                val earnedScore = if (isCorrect) question.score else 0.0
                
                android.util.Log.d("TestRepository", """
                    [FILL_BLANK]
                    Question: ${question.content}
                    Student Text: "$studentText"
                    Correct Answers: ${correctOptions.map { it.content }}
                    Match: $isCorrect
                    Result: isCorrect=$isCorrect, score=$earnedScore
                """.trimIndent())
                
                Pair(isCorrect, earnedScore)
            }
            QuestionType.ESSAY -> {
                // Essay needs manual grading
                Pair(false, 0.0)
            }
        }
    }

    override fun getStudentResult(studentId: String, testId: String): Flow<Resource<StudentTestResult?>> = flow {
        try {
            emit(Resource.Loading())
            
            // Try to fetch from Firebase first
            when (val remote = firebaseDataSource.getStudentResult(studentId, testId)) {
                is Resource.Success -> {
                    val res = remote.data
                    if (res != null) {
                        // Save to Room cache
                        studentTestResultDao.insert(res.toEntity())
                        emit(Resource.Success(res))
                    } else {
                        // Firebase returns null → Check Room cache
                        val cached = studentTestResultDao.getResultByStudentAndTest(studentId, testId)
                        if (cached != null) {
                            emit(Resource.Success(cached.toDomain()))
                        } else {
                            emit(Resource.Success(null))
                        }
                    }
                }
                is Resource.Error -> {
                    // Network error → Fallback to Room cache
                    val cached = studentTestResultDao.getResultByStudentAndTest(studentId, testId)
                    if (cached != null) {
                        emit(Resource.Success(cached.toDomain()))
                    } else {
                        emit(Resource.Error(remote.message))
                    }
                }
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy kết quả: ${e.message}"))
        }
    }

    override fun getResultsByTest(testId: String): Flow<Resource<List<StudentTestResult>>> = flow {
        try {
            android.util.Log.d("TestRepository", ">>> getResultsByTest START - testId: $testId")
            emit(Resource.Loading())
            
            when (val remote = firebaseDataSource.getResultsByTest(testId)) {
                is Resource.Success -> {
                    val list = remote.data ?: emptyList()
                    android.util.Log.d("TestRepository", ">>> Firebase returned ${list.size} results for test $testId")
                    
                    // Cache to Room
                    list.forEach { studentTestResultDao.insert(it.toEntity()) }
                    emit(Resource.Success(list))
                }
                is Resource.Error -> {
                    android.util.Log.e("TestRepository", ">>> Firebase error: ${remote.message}")
                    // Fallback to Room cache
                    val cachedResults = studentTestResultDao.getResultsByTest(testId).map { it.toDomain() }
                    android.util.Log.d("TestRepository", ">>> Fallback: Found ${cachedResults.size} cached results")
                    emit(Resource.Success(cachedResults))
                }
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            android.util.Log.e("TestRepository", ">>> Fatal error: ${e.message}", e)
            emit(Resource.Error("Lỗi khi lấy danh sách kết quả: ${e.message}"))
        }
    }

    override fun getStudentTestResults(studentId: String): Flow<Resource<List<StudentTestResult>>> = flow {
        try {
            android.util.Log.d("TestRepository", ">>> getStudentTestResults START - studentId: $studentId")
            emit(Resource.Loading())
            
            // Fetch from Firebase
            when (val result = firebaseDataSource.getResultsByStudent(studentId)) {
                is Resource.Success -> {
                    val results = result.data ?: emptyList()
                    android.util.Log.d("TestRepository", ">>> Firebase returned ${results.size} results for student $studentId")
                    
                    // Cache to Room
                    results.forEach { testResult ->
                        try {
                            studentTestResultDao.insert(testResult.toEntity())
                        } catch (e: Exception) {
                            android.util.Log.w("TestRepository", "Failed to cache result ${testResult.id}: ${e.message}")
                        }
                    }
                    
                    emit(Resource.Success(results))
                }
                is Resource.Error -> {
                    android.util.Log.e("TestRepository", ">>> Firebase error: ${result.message}")
                    // Fallback to Room cache
                    val cachedResults = studentTestResultDao.getResultsByStudent(studentId).map { it.toDomain() }
                    android.util.Log.d("TestRepository", ">>> Fallback: Found ${cachedResults.size} cached results")
                    emit(Resource.Success(cachedResults))
                }
                else -> {}
            }
        } catch (e: Exception) {
            android.util.Log.e("TestRepository", ">>> Fatal error: ${e.message}", e)
            emit(Resource.Error("Lỗi khi lấy danh sách kết quả: ${e.message}"))
        }
    }

    override fun getTestQuestions(testId: String): Flow<Resource<List<TestQuestion>>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getTestQuestions(testId)) {
                is Resource.Success -> {
                    val questions = result.data ?: emptyList()
                    questions.forEach { testQuestionDao.insert(it.toEntity()) }
                    emit(Resource.Success(questions))
                }
                is Resource.Error -> {
                    // Fallback to cached data
                    val cachedQuestions = testQuestionDao.getQuestionsByTest(testId).map { it.toDomain() }
                    if (cachedQuestions.isNotEmpty()) {
                        emit(Resource.Success(cachedQuestions))
                    } else {
                        emit(Resource.Error(result.message ?: "Không tìm thấy câu hỏi"))
                    }
                }
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách câu hỏi: ${e.message}"))
        }
    }

    override fun getQuestionOptions(questionId: String): Flow<Resource<List<TestOption>>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getTestOptionsByQuestion(questionId)) {
                is Resource.Success -> {
                    val options = result.data ?: emptyList()
                    options.forEach { testOptionDao.insert(it.toEntity()) }
                    emit(Resource.Success(options))
                }
                is Resource.Error -> {
                    // Fallback to cached data
                    val cachedOptions = testOptionDao.getOptionsByQuestion(questionId).map { it.toDomain() }
                    if (cachedOptions.isNotEmpty()) {
                        emit(Resource.Success(cachedOptions))
                    } else {
                        emit(Resource.Error(result.message ?: "Không tìm thấy đáp án"))
                    }
                }
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách đáp án: ${e.message}"))
        }
    }

    override fun getStudentAnswers(resultId: String): Flow<Resource<List<StudentTestAnswer>>> = flow {
        try {
            android.util.Log.d("TestRepository", ">>> getStudentAnswers START - resultId: $resultId")
            emit(Resource.Loading())
            
            // Fetch from Firebase
            when (val firebaseResult = firebaseDataSource.getAnswersByResultId(resultId)) {
                is Resource.Success -> {
                    val firebaseAnswers = firebaseResult.data ?: emptyList()
                    android.util.Log.d("TestRepository", ">>> Firebase returned ${firebaseAnswers.size} answers")
                    
                    // Cache to Room
                    if (firebaseAnswers.isNotEmpty()) {
                        try {
                            studentTestAnswerDao.insertAll(firebaseAnswers.map { it.toEntity() })
                        } catch (e: Exception) {
                            android.util.Log.e("TestRepository", "Error caching to Room: ${e.message}")
                        }
                    }
                    
                    // Always emit Firebase result
                    emit(Resource.Success(firebaseAnswers))
                }
                is Resource.Error -> {
                    android.util.Log.e("TestRepository", ">>> Firebase error: ${firebaseResult.message}")
                    // Fallback to Room cache
                    val answerEntities = studentTestAnswerDao.getAnswersByResultId(resultId)
                    val answers = answerEntities.map { it.toDomainModel() }
                    android.util.Log.d("TestRepository", ">>> Fallback: Found ${answers.size} cached answers")
                    
                    emit(Resource.Success(answers))
                }
                else -> {}
            }
        } catch (e: Exception) {
            android.util.Log.e("TestRepository", ">>> Fatal error: ${e.message}", e)
            emit(Resource.Error("Lỗi khi lấy câu trả lời: ${e.message}"))
        }
    }
}


