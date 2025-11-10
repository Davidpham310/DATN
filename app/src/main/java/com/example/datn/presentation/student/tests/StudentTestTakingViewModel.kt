package com.example.datn.presentation.student.tests

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.TestStatus
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StudentTestTakingViewModel @Inject constructor(
    private val testUseCases: com.example.datn.domain.usecase.test.TestUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentTestTakingState, StudentTestTakingEvent>(
    StudentTestTakingState(),
    notificationManager
) {

    companion object {
        private const val TAG = "StudentTestTakingVM"
        private const val AUTO_SAVE_INTERVAL = 30000L // 30 seconds
    }

    private var autoSaveJob: Job? = null
    private var timerJob: Job? = null

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    override fun onEvent(event: StudentTestTakingEvent) {
        when (event) {
            is StudentTestTakingEvent.LoadTest -> loadTest(event.testId)
            StudentTestTakingEvent.NextQuestion -> navigateToNextQuestion()
            StudentTestTakingEvent.PreviousQuestion -> navigateToPreviousQuestion()
            is StudentTestTakingEvent.GoToQuestion -> navigateToQuestion(event.index)
            is StudentTestTakingEvent.AnswerSingleChoice -> answerSingleChoice(event.questionId, event.optionId)
            is StudentTestTakingEvent.AnswerMultipleChoice -> answerMultipleChoice(event.questionId, event.optionIds)
            is StudentTestTakingEvent.AnswerFillBlank -> answerFillBlank(event.questionId, event.text)
            is StudentTestTakingEvent.AnswerEssay -> answerEssay(event.questionId, event.text)
            StudentTestTakingEvent.ShowSubmitDialog -> setState { copy(showSubmitDialog = true) }
            StudentTestTakingEvent.DismissSubmitDialog -> setState { copy(showSubmitDialog = false) }
            StudentTestTakingEvent.ConfirmSubmit -> submitTest()
            StudentTestTakingEvent.ToggleQuestionList -> setState { copy(showQuestionList = !showQuestionList) }
            StudentTestTakingEvent.SaveProgress -> saveProgress()
        }
    }

    private fun loadTest(testId: String) {
        Log.d(TAG, "[loadTest] START - testId: $testId")
        viewModelScope.launch {
            setState { copy(isLoading = true, startTime = System.currentTimeMillis()) }

            // Load test details first
            testUseCases.getDetails(testId).collectLatest { testResult ->
                when (testResult) {
                    is Resource.Success -> {
                        val test = testResult.data
                        if (test == null) {
                            Log.e(TAG, "[loadTest] Test not found")
                            setState { copy(isLoading = false, error = "Không tìm thấy bài kiểm tra") }
                            showNotification("Không tìm thấy bài kiểm tra", NotificationType.ERROR)
                            return@collectLatest
                        }
                        
                        Log.d(TAG, "[loadTest] Test loaded - title: ${test.title}, totalScore: ${test.totalScore}")
                        setState { copy(test = test) }
                        
                        // Load questions
                        loadQuestionsAndOptions(testId)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "[loadTest] Error loading test: ${testResult.message}")
                        setState { copy(isLoading = false, error = testResult.message) }
                        showNotification(testResult.message, NotificationType.ERROR)
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun loadQuestionsAndOptions(testId: String) {
        Log.d(TAG, "[loadQuestionsAndOptions] START - testId: $testId")
        
        testUseCases.getTestQuestions(testId).collectLatest { questionsResult ->
            when (questionsResult) {
                is Resource.Success -> {
                    val questions = questionsResult.data ?: emptyList()
                    Log.d(TAG, "[loadQuestionsAndOptions] Loaded ${questions.size} questions")
                    
                    if (questions.isEmpty()) {
                        Log.w(TAG, "[loadQuestionsAndOptions] No questions found")
                        setState { copy(isLoading = false, error = "Bài kiểm tra chưa có câu hỏi") }
                        showNotification("Bài kiểm tra chưa có câu hỏi", NotificationType.ERROR)
                        return@collectLatest
                    }
                    
                    // Load options for each question
                    val questionsWithOptions = questions.map { question ->
                        Log.d(TAG, "[loadQuestionsAndOptions] Loading options for question: ${question.id}")
                        
                        // Use try-catch to handle flow cancellation properly
                        val options = try {
                            val optionsFlow = testUseCases.getQuestionOptions(question.id)
                            var result: Resource<List<com.example.datn.domain.models.TestOption>>? = null
                            optionsFlow.collect { resource ->
                                when (resource) {
                                    is Resource.Success -> {
                                        result = resource
                                    }
                                    is Resource.Error -> {
                                        Log.e(TAG, "[loadQuestionsAndOptions] Error loading options: ${resource.message}")
                                        result = resource
                                    }
                                    else -> {}
                                }
                            }
                            (result as? Resource.Success)?.data ?: emptyList()
                        } catch (e: Exception) {
                            Log.e(TAG, "[loadQuestionsAndOptions] Exception loading options: ${e.message}")
                            emptyList()
                        }
                        
                        Log.d(TAG, "[loadQuestionsAndOptions] Loaded ${options.size} options for question ${question.id}")
                        QuestionWithOptions(
                            question = question, 
                            options = options
                        )
                    }
                    
                    Log.d(TAG, "[loadQuestionsAndOptions] All questions with options loaded successfully")
                    setState {
                        copy(
                            questions = questionsWithOptions,
                            isLoading = false,
                            error = null
                        )
                    }
                    
                    startTimer()
                    startAutoSave()
                }
                is Resource.Error -> {
                    Log.e(TAG, "[loadQuestionsAndOptions] Error: ${questionsResult.message}")
                    setState {
                        copy(
                            isLoading = false,
                            error = questionsResult.message
                        )
                    }
                    showNotification(questionsResult.message, NotificationType.ERROR)
                }
                else -> {}
            }
        }
    }

    private fun startTimer() {
        val test = state.value.test ?: return
        Log.d(TAG, "[startTimer] Starting timer - endTime: ${test.endTime}")
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = Instant.now()
                val remaining = Duration.between(now, test.endTime).seconds

                if (remaining <= 0) {
                    Log.w(TAG, "[startTimer] Time's up! Auto-submitting test")
                    // Auto-submit when time's up
                    submitTest()
                    break
                }

                setState { copy(timeRemaining = remaining) }
                delay(1000) // Update every second
            }
        }
    }

    private fun startAutoSave() {
        Log.d(TAG, "[startAutoSave] Starting auto-save every ${AUTO_SAVE_INTERVAL}ms")
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_SAVE_INTERVAL)
                saveProgress()
            }
        }
    }

    private fun navigateToNextQuestion() {
        val state = state.value
        if (state.canGoNext) {
            setState { copy(currentQuestionIndex = currentQuestionIndex + 1) }
        }
    }

    private fun navigateToPreviousQuestion() {
        val state = state.value
        if (state.canGoPrevious) {
            setState { copy(currentQuestionIndex = currentQuestionIndex - 1) }
        }
    }

    private fun navigateToQuestion(index: Int) {
        val state = state.value
        if (index in state.questions.indices) {
            setState { copy(currentQuestionIndex = index) }
        }
    }

    private fun answerSingleChoice(questionId: String, optionId: String) {
        Log.d(TAG, "[answerSingleChoice] questionId: $questionId, optionId: $optionId")
        setState {
            copy(
                answers = answers + (questionId to Answer.SingleChoice(optionId)),
                lastSavedTime = 0L // Mark as unsaved
            )
        }
    }

    private fun answerMultipleChoice(questionId: String, optionIds: Set<String>) {
        Log.d(TAG, "[answerMultipleChoice] questionId: $questionId, optionIds: $optionIds")
        setState {
            copy(
                answers = answers + (questionId to Answer.MultipleChoice(optionIds)),
                lastSavedTime = 0L
            )
        }
    }

    private fun answerFillBlank(questionId: String, text: String) {
        Log.d(TAG, "[answerFillBlank] questionId: $questionId, text length: ${text.length}")
        setState {
            copy(
                answers = answers + (questionId to Answer.FillBlank(text)),
                lastSavedTime = 0L
            )
        }
    }

    private fun answerEssay(questionId: String, text: String) {
        Log.d(TAG, "[answerEssay] questionId: $questionId, text length: ${text.length}")
        setState {
            copy(
                answers = answers + (questionId to Answer.Essay(text)),
                lastSavedTime = 0L
            )
        }
    }

    private fun saveProgress() {
        val currentState = state.value
        Log.d(TAG, "[saveProgress] Saving ${currentState.answers.size} answers")
        
        // TODO: Implement save to backend
        // For now, just update the last saved time
        setState { copy(lastSavedTime = System.currentTimeMillis()) }
        Log.d(TAG, "[saveProgress] Progress saved successfully")
    }

    private fun submitTest() {
        Log.d(TAG, "[submitTest] START")
        viewModelScope.launch {
            val state = state.value
            val test = state.test
            val questions = state.questions
            val answers = state.answers

            if (test == null) {
                Log.e(TAG, "[submitTest] Test is null")
                return@launch
            }

            Log.d(TAG, "[submitTest] Test: ${test.title}, Answers: ${answers.size}/${questions.size}")
            setState { copy(isSubmitting = true, showSubmitDialog = false) }

            // Get student ID
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.first { it.isNotBlank() }
            }

            Log.d(TAG, "[submitTest] Current user ID: $currentUserId")

            // Get student profile with proper flow handling
            var tempStudentId: String? = null
            try {
                getStudentProfileByUserId(currentUserId).collect { profileResult ->
                    when (profileResult) {
                        is Resource.Success -> {
                            tempStudentId = profileResult.data?.id
                            Log.d(TAG, "[submitTest] Got student ID: $tempStudentId")
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "[submitTest] Error getting profile: ${profileResult.message}")
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[submitTest] Exception getting profile: ${e.message}")
            }

            val studentId = tempStudentId
            if (studentId == null) {
                Log.e(TAG, "[submitTest] Student ID not found for userId: $currentUserId")
                setState { copy(isSubmitting = false) }
                showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                return@launch
            }

            Log.d(TAG, "[submitTest] Student ID: $studentId")

            // Calculate duration
            val duration = (System.currentTimeMillis() - state.startTime) / 1000
            Log.d(TAG, "[submitTest] Duration: ${duration}s")

            // Grade the test
            var totalScore = 0.0
            val gradingLog = mutableListOf<String>()
            
            questions.forEach { questionWithOptions ->
                val answer = answers[questionWithOptions.question.id]
                if (answer != null) {
                    val score = gradeQuestion(
                        questionWithOptions.question,
                        questionWithOptions.options,
                        answer
                    )
                    totalScore += score
                    gradingLog.add("Q${questionWithOptions.question.order}: ${score}/${questionWithOptions.question.score}")
                } else {
                    gradingLog.add("Q${questionWithOptions.question.order}: 0/${questionWithOptions.question.score} (unanswered)")
                }
            }
            
            Log.d(TAG, "[submitTest] Grading complete - Total: $totalScore/${test.totalScore}")
            gradingLog.forEach { Log.d(TAG, "[submitTest] $it") }

            // Create result object
            val result = StudentTestResult(
                id = UUID.randomUUID().toString(),
                studentId = studentId,
                testId = test.id,
                score = totalScore,
                completionStatus = TestStatus.GRADED,
                submissionTime = Instant.now(),
                durationSeconds = duration,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            
            // Convert answers to Map<String, Any> for repository
            val answersMap = answers.mapValues { (_, answer) ->
                when (answer) {
                    is Answer.SingleChoice -> answer.optionId
                    is Answer.MultipleChoice -> answer.optionIds.toList()
                    is Answer.FillBlank -> answer.text
                    is Answer.Essay -> answer.text
                }
            }
            
            Log.d(TAG, "[submitTest] Submitting to repository...")
            testUseCases.submitTestResult(result, answersMap).collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        Log.d(TAG, "[submitTest] SUCCESS - Result ID: ${resource.data?.id}")
                        setState { copy(isSubmitting = false, isSubmitted = true) }
                        showNotification("Đã nộp bài thành công! Điểm: $totalScore/${test.totalScore}", NotificationType.SUCCESS)
                        // Navigation will be handled by the screen
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "[submitTest] ERROR: ${resource.message}")
                        setState { copy(isSubmitting = false) }
                        showNotification(resource.message, NotificationType.ERROR)
                    }
                    is Resource.Loading -> {
                        Log.d(TAG, "[submitTest] Loading...")
                    }
                }
            }
        }
    }

    private fun gradeQuestion(
        question: com.example.datn.domain.models.TestQuestion,
        options: List<com.example.datn.domain.models.TestOption>,
        answer: Answer
    ): Double {
        val score = when (question.questionType) {
            QuestionType.SINGLE_CHOICE -> {
                val selectedId = (answer as? Answer.SingleChoice)?.optionId
                val correctOption = options.find { it.isCorrect }
                if (selectedId == correctOption?.id) question.score else 0.0
            }

            QuestionType.MULTIPLE_CHOICE -> {
                val selectedIds = (answer as? Answer.MultipleChoice)?.optionIds ?: emptySet()
                val correctIds = options.filter { it.isCorrect }.map { it.id }.toSet()
                if (selectedIds == correctIds) question.score else 0.0
            }

            QuestionType.FILL_BLANK -> {
                val text = (answer as? Answer.FillBlank)?.text ?: ""
                val correctAnswer = options.firstOrNull()?.content ?: ""
                if (text.trim().equals(correctAnswer.trim(), ignoreCase = true))
                    question.score
                else 0.0
            }

            QuestionType.ESSAY -> {
                // Essay needs manual grading by teacher
                0.0
            }
        }
        
        Log.d(TAG, "[gradeQuestion] Q${question.order} (${question.questionType}): $score/${question.score}")
        return score
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        autoSaveJob?.cancel()
        // Final save before exit
        viewModelScope.launch {
            saveProgress()
        }
    }
}
