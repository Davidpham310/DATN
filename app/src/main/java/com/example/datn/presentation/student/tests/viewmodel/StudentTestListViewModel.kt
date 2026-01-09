package com.example.datn.presentation.student.tests.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestStatus
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.domain.usecase.test.TestUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.student.tests.event.StudentTestListEvent
import com.example.datn.presentation.student.tests.state.StudentTestListState
import com.example.datn.presentation.student.tests.state.TestWithStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class StudentTestListViewModel @Inject constructor(
    private val testUseCases: TestUseCases,
    private val classUseCases: ClassUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentTestListState, StudentTestListEvent>(
    StudentTestListState(),
    notificationManager
) {

    companion object {
        private const val TAG = "StudentTestListVM"
    }

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    init {
        loadTests()
    }

    override fun onEvent(event: StudentTestListEvent) {
        when (event) {
            StudentTestListEvent.LoadTests -> loadTests()
            StudentTestListEvent.RefreshTests -> refreshTests()
            is StudentTestListEvent.RequestStartTest -> handleRequestStartTest(event.testId)
            is StudentTestListEvent.NavigateToTest -> {
                // Navigation handled by screen
            }
            is StudentTestListEvent.NavigateToResult -> {
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

    private fun loadTests() {
        Log.d(TAG, "[loadTests] START")
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }

            // Get student ID
            val currentUserId = currentUserIdFlow.value.ifBlank {
                awaitNonBlank(currentUserIdFlow)
            }
            Log.d(TAG, "[loadTests] Current user ID: $currentUserId")

            getStudentProfileByUserId(currentUserId).collect { profileResult ->
                when (profileResult) {
                    is Resource.Success -> {
                        val studentId = profileResult.data?.id
                        Log.d(TAG, "[loadTests] Student ID: $studentId")
                        if (studentId.isNullOrBlank()) {
                            setState {
                                copy(
                                    isLoading = false,
                                    error = "Không tìm thấy thông tin học sinh"
                                )
                            }
                            showNotification(
                                "Không tìm thấy thông tin học sinh",
                                NotificationType.ERROR
                            )
                            return@collect
                        }

                        loadTestsForStudent(studentId)
                    }
                    is Resource.Error -> {
                        setState {
                            copy(isLoading = false, error = profileResult.message)
                        }
                        showNotification(profileResult.message, NotificationType.ERROR)
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun loadTestsForStudent(studentId: String) {
        Log.d(TAG, "[loadTestsForStudent] START - studentId: $studentId")
        // Get student's classes
        classUseCases.getClassesByStudent(studentId).collect { classesResult ->
            when (classesResult) {
                is Resource.Success -> {
                    val classIds = classesResult.data?.map { it.id } ?: emptyList()
                    Log.d(TAG, "[loadTestsForStudent] Found ${classIds.size} classes")

                    if (classIds.isEmpty()) {
                        setState {
                            copy(
                                isLoading = false,
                                tests = emptyList(),
                                upcomingTests = emptyList(),
                                ongoingTests = emptyList(),
                                overdueTests = emptyList(),
                                completedTests = emptyList()
                            )
                        }
                        showNotification("Hiện chưa có bài kiểm tra", NotificationType.INFO)
                        return@collect
                    }

                    // Get tests for these classes
                    Log.d(TAG, "[loadTestsForStudent] Loading tests for classes: $classIds")
                    testUseCases.getTestsByClasses(classIds).collect { testsResult ->
                        when (testsResult) {
                            is Resource.Success -> {
                                val tests = testsResult.data ?: emptyList()
                                Log.d(TAG, "[loadTestsForStudent] Loaded ${tests.size} tests")

                                if (tests.isEmpty()) {
                                    setState {
                                        copy(
                                            isLoading = false,
                                            tests = emptyList(),
                                            upcomingTests = emptyList(),
                                            ongoingTests = emptyList(),
                                            overdueTests = emptyList(),
                                            completedTests = emptyList(),
                                            error = null
                                        )
                                    }
                                    showNotification("Hiện chưa có bài kiểm tra", NotificationType.INFO)
                                    return@collect
                                }

                                processTests(tests, studentId)
                            }
                            is Resource.Error -> {
                                setState {
                                    copy(isLoading = false, error = testsResult.message)
                                }
                                showNotification(testsResult.message, NotificationType.ERROR)
                            }
                            else -> {}
                        }
                    }
                }
                is Resource.Error -> {
                    setState {
                        copy(isLoading = false, error = classesResult.message)
                    }
                    showNotification(classesResult.message, NotificationType.ERROR)
                }
                else -> {}
            }
        }
    }

    private fun refreshTests() {
        loadTests()
    }

    private fun handleRequestStartTest(testId: String) {
        val testWithStatus = state.value.tests.firstOrNull { it.test.id == testId }
        if (testWithStatus == null) {
            showNotification("Không tìm thấy bài kiểm tra", NotificationType.ERROR)
            return
        }

        if (testWithStatus.hasResult) {
            val resultId = testWithStatus.result?.id
            if (resultId != null) {
                sendEvent(StudentTestListEvent.NavigateToResult(testId = testId, resultId = resultId))
            } else {
                showNotification("Không tìm thấy kết quả bài kiểm tra", NotificationType.ERROR)
            }
            return
        }

        val now = Instant.now()
        when {
            now.isBefore(testWithStatus.test.startTime) -> {
                showNotification("Bài kiểm tra chưa được mở", NotificationType.ERROR)
            }
            !now.isBefore(testWithStatus.test.endTime) -> {
                showNotification("Bài kiểm tra đã kết thúc", NotificationType.ERROR)
            }
            else -> {
                sendEvent(StudentTestListEvent.NavigateToTest(testId))
            }
        }
    }

    private suspend fun processTests(
        tests: List<com.example.datn.domain.models.Test>,
        studentId: String
    ) {
        Log.d(TAG, "[processTests] Processing ${tests.size} tests for student $studentId")
        // Get student's test results
        testUseCases.getStudentTestResults(studentId).collect { resultsResult ->
            when (resultsResult) {
                is Resource.Success -> {
                    val results = resultsResult.data ?: emptyList()
                    Log.d(TAG, "[processTests] Found ${results.size} test results")
                    
                    val testsWithStatus = tests.map { test ->
                        val result = results.find { it.testId == test.id }
                        calculateTestStatus(test, result)
                    }
                    
                    categorizeTests(testsWithStatus)
                }
                is Resource.Error -> {
                    // Even if we can't get results, show tests
                    val testsWithStatus = tests.map { test ->
                        calculateTestStatus(test, null)
                    }
                    categorizeTests(testsWithStatus)
                }
                else -> {}
            }
        }
    }

    private fun calculateTestStatus(
        test: com.example.datn.domain.models.Test,
        result: com.example.datn.domain.models.StudentTestResult?
    ): TestWithStatus {
        val now = Instant.now()
        
        val status = when {
            result != null -> result.completionStatus
            now.isBefore(test.startTime) -> TestStatus.UNSUBMITTED
            !now.isBefore(test.endTime) -> TestStatus.OVERDUE
            else -> TestStatus.UNSUBMITTED
        }
        
        val isOverdue = !now.isBefore(test.endTime) && result == null
        val canTake = !now.isBefore(test.startTime) &&
                     now.isBefore(test.endTime) &&
                     (result == null || result.completionStatus != TestStatus.GRADED)
        
        return TestWithStatus(
            test = test,
            result = result,
            status = status,
            timeRemaining = calculateTimeRemaining(test),
            isOverdue = isOverdue,
            canTakeTest = canTake
        )
    }

    private fun calculateTimeRemaining(test: com.example.datn.domain.models.Test): String? {
        val now = Instant.now()
        return when {
            now.isBefore(test.startTime) -> {
                val days = java.time.Duration.between(now, test.startTime).toDays()
                "Còn $days ngày"
            }
            !now.isBefore(test.endTime) -> null
            else -> {
                val hours = java.time.Duration.between(now, test.endTime).toHours()
                when {
                    hours > 24 -> "Còn ${hours / 24} ngày"
                    hours > 0 -> "Còn $hours giờ"
                    else -> {
                        val minutes = java.time.Duration.between(now, test.endTime).toMinutes()
                        "Còn $minutes phút"
                    }
                }
            }
        }
    }

    private fun categorizeTests(testsWithStatus: List<TestWithStatus>) {
        Log.d(TAG, "[categorizeTests] Categorizing ${testsWithStatus.size} tests")
        val now = Instant.now()
        val completedStatuses = listOf(TestStatus.COMPLETED, TestStatus.GRADED, TestStatus.SUBMITTED)

        val upcoming = testsWithStatus.filter {
            it.test.startTime.isAfter(now)
        }.sortedBy { it.test.startTime }

        val ongoing = testsWithStatus.filter {
            !it.test.startTime.isAfter(now) &&
            it.test.endTime.isAfter(now) &&
            it.status !in completedStatuses
        }.sortedBy { it.test.endTime }

        val completed = testsWithStatus.filter {
            it.status in completedStatuses
        }.sortedByDescending { it.result?.submissionTime }

        val overdue = testsWithStatus.filter {
            it.status == TestStatus.OVERDUE
        }.sortedByDescending { it.test.endTime }

        Log.d(TAG, "[categorizeTests] Upcoming: ${upcoming.size}, Ongoing: ${ongoing.size}, Completed: ${completed.size}")
        setState {
            copy(
                tests = testsWithStatus,
                upcomingTests = upcoming,
                ongoingTests = ongoing,
                overdueTests = overdue,
                completedTests = completed,
                isLoading = false,
                error = null
            )
        }
    }
}
