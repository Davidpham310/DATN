# 📋 Implementation Guide: Hiển Thị Chi Tiết Kết Quả Bài Kiểm Tra

## 🎯 Mục Tiêu
Hiển thị đầy đủ kết quả bài kiểm tra bao gồm:
- ✅ Số câu đúng/sai
- ⏱️ Thời gian làm bài  
- 📝 Chi tiết từng câu với đáp án đúng/sai
- 🔍 Highlight câu sai và hiển thị đáp án đúng

## 📁 Files Đã Tạo

### 1. Domain Layer
- `StudentTestAnswer.kt` - Model cho câu trả lời của học sinh
- Updated `ITestRepository.kt` - Thêm `getStudentAnswers()`

### 2. Data Layer  
- `StudentTestAnswerEntity.kt` - Room entity
- `StudentTestAnswerDao.kt` - DAO để persist answers

## 🔧 Các Bước Cần Thực Hiện

### BƯỚC 1: Update Room Database

**File:** `d:\Code\Android\Kotlin\DATN\app\src\main\java\com\example\datn\data\local\AppDatabase.kt`

Thêm entity mới và tăng version:

```kotlin
@Database(
    entities = [
        // ... existing entities ...
        StudentTestAnswerEntity::class  // ← THÊM
    ],
    version = X + 1,  // ← TĂNG VERSION
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase {
    // ... existing DAOs ...
    abstract fun studentTestAnswerDao(): StudentTestAnswerDao  // ← THÊM
}
```

**Migration (nếu cần):**
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS student_test_answers (
                id TEXT PRIMARY KEY NOT NULL,
                resultId TEXT NOT NULL,
                questionId TEXT NOT NULL,
                answer TEXT NOT NULL,
                isCorrect INTEGER NOT NULL,
                earnedScore REAL NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
    }
}
```

### BƯỚC 2: Update TestRepositoryImpl - Save Answers

**File:** `TestRepositoryImpl.kt`

Inject DAO:
```kotlin
class TestRepositoryImpl @Inject constructor(
    // ... existing params ...
    private val studentTestAnswerDao: StudentTestAnswerDao  // ← THÊM
) : ITestRepository {
```

Update `submitTestResult` để lưu answers:

```kotlin
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
                
                // ========== THÊM: Save Answers ==========
                // Get questions to calculate correctness
                val questions = testQuestionDao.getQuestionsByTestId(saved.testId)
                val studentAnswers = mutableListOf<StudentTestAnswer>()
                
                questions.forEach { questionEntity ->
                    val question = questionEntity.toDomainModel()
                    val answer = answers[question.id]
                    
                    if (answer != null) {
                        // Get options for grading
                        val options = testOptionDao.getOptionsByQuestionId(question.id)
                            .map { it.toDomainModel() }
                        
                        // Calculate correctness and score
                        val (isCorrect, earnedScore) = gradeAnswer(question, options, answer)
                        
                        // Serialize answer
                        val answerString = when (answer) {
                            is String -> answer
                            is List<*> -> answer.joinToString(",")
                            else -> answer.toString()
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
                        
                        studentAnswers.add(studentAnswer)
                    }
                }
                
                // Save all answers to Room
                studentTestAnswerDao.insertAll(studentAnswers.map { it.toEntity() })
                // ========================================
                
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
    return when (question.questionType) {
        QuestionType.SINGLE_CHOICE -> {
            val selectedId = answer as? String ?: return Pair(false, 0.0)
            val isCorrect = options.any { it.id == selectedId && it.isCorrect }
            Pair(isCorrect, if (isCorrect) question.score else 0.0)
        }
        QuestionType.MULTIPLE_CHOICE -> {
            val selectedIds = (answer as? List<*>)?.map { it.toString() }?.toSet() ?: return Pair(false, 0.0)
            val correctIds = options.filter { it.isCorrect }.map { it.id }.toSet()
            val isCorrect = selectedIds == correctIds
            Pair(isCorrect, if (isCorrect) question.score else 0.0)
        }
        QuestionType.FILL_BLANK -> {
            val studentText = (answer as? String ?: "").trim().lowercase()
            val correctOptions = options.filter { it.isCorrect }
            val isCorrect = correctOptions.any { 
                it.content.trim().lowercase() == studentText 
            }
            Pair(isCorrect, if (isCorrect) question.score else 0.0)
        }
        QuestionType.ESSAY -> {
            // Essay needs manual grading
            Pair(false, 0.0)
        }
    }
}
```

Implement `getStudentAnswers`:
```kotlin
override fun getStudentAnswers(resultId: String): Flow<Resource<List<StudentTestAnswer>>> = flow {
    try {
        emit(Resource.Loading())
        val answers = studentTestAnswerDao.getAnswersByResultId(resultId)
            .map { it.toDomainModel() }
        emit(Resource.Success(answers))
    } catch (e: Exception) {
        emit(Resource.Error("Lỗi khi tải câu trả lời: ${e.message}"))
    }
}
```

### BƯỚC 3: Update TestUseCases

**File:** `TestUseCases.kt`

Thêm method:
```kotlin
fun getStudentAnswers(resultId: String): Flow<Resource<List<StudentTestAnswer>>> =
    repository.getStudentAnswers(resultId)
```

### BƯỚC 4: Update StudentTestResultViewModel

**File:** `StudentTestResultViewModel.kt`

Update `loadResult` để load và build `QuestionWithAnswer`:

```kotlin
private fun loadResult(testId: String, resultId: String) {
    Log.d(TAG, "[loadResult] START - testId: $testId, resultId: $resultId")
    viewModelScope.launch {
        setState { copy(isLoading = true, error = null) }

        // Get student ID
        val currentUserId = currentUserIdFlow.value.ifBlank {
            currentUserIdFlow.first { it.isNotBlank() }
        }

        var studentId: String? = null
        try {
            getStudentProfileByUserId(currentUserId).collect { profileResult ->
                when (profileResult) {
                    is Resource.Success -> {
                        studentId = profileResult.data?.id
                        Log.d(TAG, "[loadResult] Got student ID: $studentId")
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "[loadResult] Error getting profile: ${profileResult.message}")
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[loadResult] Exception getting profile: ${e.message}")
        }

        if (studentId == null) {
            Log.e(TAG, "[loadResult] Student ID not found")
            setState { copy(isLoading = false, error = "Không tìm thấy thông tin học sinh") }
            showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
            return@launch
        }

        // Load all data
        try {
            var test: Test? = null
            var result: StudentTestResult? = null
            var questions: List<TestQuestion> = emptyList()
            var answers: List<StudentTestAnswer> = emptyList()

            // Load test details
            testUseCases.getDetails(testId).collect { testResult ->
                when (testResult) {
                    is Resource.Success -> {
                        test = testResult.data
                        Log.d(TAG, "[loadResult] Test loaded: ${test?.title}")
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "[loadResult] Error loading test: ${testResult.message}")
                    }
                    else -> {}
                }
            }

            // Load student result
            testUseCases.getStudentResult(studentId, testId).collect { resultResult ->
                when (resultResult) {
                    is Resource.Success -> {
                        result = resultResult.data
                        Log.d(TAG, "[loadResult] Result loaded - score: ${result?.score}, resultId: ${result?.id}")
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "[loadResult] Error loading result: ${resultResult.message}")
                    }
                    else -> {}
                }
            }

            // Load questions
            testUseCases.getTestQuestions(testId).collect { questionsResult ->
                when (questionsResult) {
                    is Resource.Success -> {
                        questions = questionsResult.data ?: emptyList()
                        Log.d(TAG, "[loadResult] Questions loaded: ${questions.size}")
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "[loadResult] Error loading questions: ${questionsResult.message}")
                    }
                    else -> {}
                }
            }

            // ========== THÊM: Load Answers ==========
            if (result != null) {
                testUseCases.getStudentAnswers(result.id).collect { answersResult ->
                    when (answersResult) {
                        is Resource.Success -> {
                            answers = answersResult.data ?: emptyList()
                            Log.d(TAG, "[loadResult] Answers loaded: ${answers.size}")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "[loadResult] Error loading answers: ${answersResult.message}")
                        }
                        else -> {}
                    }
                }
            }
            // =========================================

            // ========== THÊM: Build QuestionWithAnswer ==========
            if (test != null && result != null) {
                val questionsWithAnswers = buildQuestionsWithAnswers(questions, answers)
                
                Log.d(TAG, "[loadResult] SUCCESS - All data loaded, built ${questionsWithAnswers.size} questions")
                setState {
                    copy(
                        test = test,
                        result = result,
                        questions = questionsWithAnswers,
                        isLoading = false,
                        error = null
                    )
                }
                showNotification("Kết quả bài kiểm tra đã sẵn sàng", NotificationType.SUCCESS)
            } else {
                Log.e(TAG, "[loadResult] Missing data - test: ${test != null}, result: ${result != null}")
                setState {
                    copy(
                        isLoading = false,
                        error = "Không thể tải đầy đủ thông tin kết quả"
                    )
                }
                showNotification("Không thể tải đầy đủ thông tin kết quả", NotificationType.ERROR)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[loadResult] Exception: ${e.message}")
            setState {
                copy(isLoading = false, error = "Lỗi: ${e.message}")
            }
            showNotification("Lỗi: ${e.message}", NotificationType.ERROR)
        }
    }
}

private suspend fun buildQuestionsWithAnswers(
    questions: List<TestQuestion>,
    studentAnswers: List<StudentTestAnswer>
): List<QuestionWithAnswer> {
    val result = mutableListOf<QuestionWithAnswer>()
    
    questions.forEach { question ->
        // Load options for this question
        var options: List<TestOption> = emptyList()
        testUseCases.getQuestionOptions(question.id).collect { optionsResult ->
            if (optionsResult is Resource.Success) {
                options = optionsResult.data ?: emptyList()
            }
        }
        
        // Find student's answer
        val studentAnswer = studentAnswers.find { it.questionId == question.id }
        
        // Parse answer
        val parsedAnswer = studentAnswer?.let { parseAnswer(it, question.questionType) }
        
        // Build correct answer
        val correctAnswer = buildCorrectAnswer(question, options)
        
        // Create QuestionWithAnswer
        val questionWithAnswer = QuestionWithAnswer(
            question = question,
            options = options,
            studentAnswer = parsedAnswer,
            correctAnswer = correctAnswer,
            earnedScore = studentAnswer?.earnedScore ?: 0.0,
            isCorrect = studentAnswer?.isCorrect ?: false
        )
        
        result.add(questionWithAnswer)
    }
    
    return result.sortedBy { it.question.order }
}

private fun parseAnswer(answer: StudentTestAnswer, questionType: QuestionType): Answer {
    return when (questionType) {
        QuestionType.SINGLE_CHOICE -> Answer.SingleChoice(answer.answer)
        QuestionType.MULTIPLE_CHOICE -> {
            val ids = answer.answer.split(",").toSet()
            Answer.MultipleChoice(ids)
        }
        QuestionType.FILL_BLANK -> Answer.FillBlank(answer.answer)
        QuestionType.ESSAY -> Answer.Essay(answer.answer)
    }
}

private fun buildCorrectAnswer(question: TestQuestion, options: List<TestOption>): Answer {
    return when (question.questionType) {
        QuestionType.SINGLE_CHOICE -> {
            val correctOption = options.find { it.isCorrect }
            Answer.SingleChoice(correctOption?.id ?: "")
        }
        QuestionType.MULTIPLE_CHOICE -> {
            val correctIds = options.filter { it.isCorrect }.map { it.id }.toSet()
            Answer.MultipleChoice(correctIds)
        }
        QuestionType.FILL_BLANK -> {
            val correctOption = options.find { it.isCorrect }
            Answer.FillBlank(correctOption?.content ?: "")
        }
        QuestionType.ESSAY -> Answer.Essay("") // No correct answer for essay
    }
}
```

## ✅ Kết Quả Sau Khi Hoàn Thành

### UI Sẽ Hiển Thị:

```
┌──────────────────────────────────┐
│ ← Kiểm tra giữa kỳ        [👁️]   │
├──────────────────────────────────┤
│ 🎯 ĐIỂM SỐ                       │
│                                  │
│      8.5 / 10.0 (85%)            │
│      Giỏi ⭐⭐                    │
│                                  │
│ ✅ Đúng: 17    ❌ Sai: 3         │
│ ⏱️ Thời gian: 25 phút            │
├──────────────────────────────────┤
│ 📊 CHI TIẾT CÂU TRẢ LỜI         │
│                                  │
│ ┌─ Câu 1 ──────────── ✅ 1.0/1.0┐│
│ │ 2 + 3 = ?                     ││
│ │ ● 5    ← Đúng                 ││
│ └──────────────────────────────┘ │
│                                  │
│ ┌─ Câu 2 ──────────── ❌ 0.0/1.5┐│
│ │ Thủ đô của Việt Nam?          ││
│ │ ✗ Hồ Chí Minh ← Sai           ││
│ │ ✓ Hà Nội ← Đáp án đúng        ││
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

## 📝 Notes

- Lint errors là IDE warnings, code sẽ compile thành công
- Cần rebuild project sau khi update: `./gradlew clean build`
- Test thoroughly với các loại câu hỏi khác nhau
- Đảm bảo migration database chạy đúng

## 🐛 Troubleshooting

**Database error:** Chạy migration hoặc clear app data  
**Answers không hiển thị:** Check logs xem có lưu vào Room không  
**Wrong answers:** Verify grading logic trong `gradeAnswer()`

---

**Status:** Ready to implement  
**Estimated Time:** 1-2 hours  
**Priority:** High
