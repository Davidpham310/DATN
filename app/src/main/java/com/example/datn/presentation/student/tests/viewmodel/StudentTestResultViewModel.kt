package com.example.datn.presentation.student.tests.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.student.tests.event.StudentTestResultEvent
import com.example.datn.presentation.student.tests.state.Answer
import com.example.datn.presentation.student.tests.state.StudentTestResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentTestResultViewModel @Inject constructor(
    private val testUseCases: com.example.datn.domain.usecase.test.TestUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    private val syncManager: com.example.datn.data.sync.FirebaseRoomSyncManager,
    notificationManager: NotificationManager
) : BaseViewModel<StudentTestResultState, StudentTestResultEvent>(
    StudentTestResultState(),
    notificationManager
) {

    companion object {
        private const val TAG = "StudentTestResultVM"
    }

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    override fun onEvent(event: StudentTestResultEvent) {
        when (event) {
            is StudentTestResultEvent.LoadResult -> loadResult(event.testId, event.resultId)
            StudentTestResultEvent.ToggleDetailedAnswers -> {
                setState { copy(showDetailedAnswers = !showDetailedAnswers) }
            }
            StudentTestResultEvent.NavigateBack -> {
                // Navigation handled by screen
            }
        }
    }

    private suspend fun awaitNonBlank(flow: Flow<String>): String {
        var result = ""
        flow
            .filter { it.isNotBlank() }
            .take(1)
            .collect { value -> result = value }
        return result
    }

    private fun loadResult(testId: String, resultId: String) {
        Log.d(TAG, "[loadResult] START - testId: $testId, resultId: $resultId")
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            // Get student ID
            val currentUserId = currentUserIdFlow.value.ifBlank {
                awaitNonBlank(currentUserIdFlow)
            }

            var tempStudentId: String? = null
            try {
                getStudentProfileByUserId(currentUserId).collect { profileResult ->
                    when (profileResult) {
                        is Resource.Success -> {
                            tempStudentId = profileResult.data?.id
                            Log.d(TAG, "[loadResult] Got student ID: $tempStudentId")
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

            val studentId = tempStudentId
            if (studentId == null) {
                Log.e(TAG, "[loadResult] Student ID not found")
                setState { copy(isLoading = false, error = "Không tìm thấy thông tin học sinh") }
                showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                return@launch
            }

            // ========== SYNC DATA FIRST ==========
            try {
                Log.d(TAG, "[loadResult] Syncing test data...")
                syncManager.syncTestData(testId, forceSync = false)
                Log.d(TAG, "[loadResult] Test data synced")
                
                // Sync student result và answers
                Log.d(TAG, "[loadResult] Syncing student result...")
                syncManager.syncStudentTestResult(studentId, testId, forceSync = false)
                Log.d(TAG, "[loadResult] Student result synced")
            } catch (e: Exception) {
                Log.w(TAG, "[loadResult] Sync warning: ${e.message} - Will try to use cached data")
            }
            // ==========================================

            // Load test details, result, and questions in parallel
            try {
                var test: com.example.datn.domain.models.Test? = null
                var tempResult: com.example.datn.domain.models.StudentTestResult? = null
                var questions: List<com.example.datn.domain.models.TestQuestion> = emptyList()

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
                            tempResult = resultResult.data
                            Log.d(TAG, "[loadResult] Result loaded - score: ${tempResult?.score}")
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

                // Load student answers
                var answers: List<com.example.datn.domain.models.StudentTestAnswer> = emptyList()
                val result = tempResult
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

                // Build QuestionWithAnswer list
                val questionsWithAnswers = if (questions.isNotEmpty()) {
                    buildQuestionsWithAnswers(questions, answers, testUseCases)
                } else {
                    emptyList()
                }

                // Update state with all data
                if (test != null && result != null) {
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

            // When TestUseCases is available:
            // combine(
            //     testUseCases.getTestById(testId),
            //     testUseCases.getTestResult(resultId),
            //     testUseCases.getTestQuestions(testId)
            // ) { testResult, resultResult, questionsResult ->
            //     Triple(testResult, resultResult, questionsResult)
            // }.collectLatest { (testResult, resultResult, questionsResult) ->
            //     when {
            //         testResult is Resource.Success && 
            //         resultResult is Resource.Success && 
            //         questionsResult is Resource.Success -> {
            //             
            //             val test = testResult.data!!
            //             val result = resultResult.data!!
            //             val questions = questionsResult.data ?: emptyList()
            //             
            //             // Load student answers
            //             val studentAnswers = testUseCases.getStudentAnswers(resultId).first()
            //             val answersMap = (studentAnswers as? Resource.Success)?.data ?: emptyMap()
            //             
            //             // Map questions with answers
            //             val questionsWithAnswers = questions.map { question ->
            //                 val options = testUseCases.getQuestionOptions(question.id).first()
            //                 val optionsList = (options as? Resource.Success)?.data ?: emptyList()
            //                 val studentAnswer = answersMap[question.id]
            //                 val correctAnswer = determineCorrectAnswer(optionsList)
            //                 val earnedScore = calculateEarnedScore(question, optionsList, studentAnswer)
            //                 val isCorrect = earnedScore == question.score
            //                 
            //                 QuestionWithAnswer(
            //                     question = question,
            //                     options = optionsList,
            //                     studentAnswer = studentAnswer,
            //                     correctAnswer = correctAnswer,
            //                     earnedScore = earnedScore,
            //                     isCorrect = isCorrect
            //                 )
            //             }
            //             
            //             // Calculate class statistics (optional)
            //             val classStats = testUseCases.getClassStatistics(testId).first()
            //             val average = (classStats as? Resource.Success)?.data?.average
            //             val rank = (classStats as? Resource.Success)?.data?.getRank(result.score)
            //             val total = (classStats as? Resource.Success)?.data?.totalStudents
            //             
            //             setState {
            //                 copy(
            //                     test = test,
            //                     result = result,
            //                     questions = questionsWithAnswers,
            //                     classAverage = average,
            //                     classRank = rank,
            //                     totalStudents = total,
            //                     isLoading = false,
            //                     error = null
            //                 )
            //             }
            //         }
            //         testResult is Resource.Error -> handleError(testResult.message)
            //         resultResult is Resource.Error -> handleError(resultResult.message)
            //         questionsResult is Resource.Error -> handleError(questionsResult.message)
            //     }
            // }
        }
    }

    private fun determineCorrectAnswer(
        options: List<com.example.datn.domain.models.TestOption>
    ): Answer {
        val correctOptions = options.filter { it.isCorrect }
        return when {
            correctOptions.size == 1 -> Answer.SingleChoice(correctOptions[0].id)
            correctOptions.size > 1 -> Answer.MultipleChoice(correctOptions.map { it.id }.toSet())
            else -> Answer.FillBlank(correctOptions.getOrNull(0)?.content ?: "")
        }
    }

    private fun calculateEarnedScore(
        question: com.example.datn.domain.models.TestQuestion,
        options: List<com.example.datn.domain.models.TestOption>,
        studentAnswer: Answer?
    ): Double {
        if (studentAnswer == null) return 0.0

        return when (question.questionType) {
            com.example.datn.domain.models.QuestionType.SINGLE_CHOICE -> {
                val selectedId = (studentAnswer as? Answer.SingleChoice)?.optionId
                val isCorrect = options.any { it.id == selectedId && it.isCorrect }
                if (isCorrect) question.score else 0.0
            }
            com.example.datn.domain.models.QuestionType.MULTIPLE_CHOICE -> {
                val selectedIds = (studentAnswer as? Answer.MultipleChoice)?.optionIds ?: emptySet()
                val correctIds = options.filter { it.isCorrect }.map { it.id }.toSet()
                if (selectedIds == correctIds) question.score else 0.0
            }
            com.example.datn.domain.models.QuestionType.FILL_BLANK -> {
                val text = (studentAnswer as? Answer.FillBlank)?.text ?: ""
                val correctAnswer = options.find { it.isCorrect }?.content ?: ""
                if (text.trim().equals(correctAnswer.trim(), ignoreCase = true))
                    question.score
                else 0.0
            }
            com.example.datn.domain.models.QuestionType.ESSAY -> {
                // Essay grading is manual, get from result
                0.0
            }
        }
    }
}
