package com.example.datn.presentation.teacher.studentdetail

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.user.UserUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Student Detail Screen
 * Loads and manages comprehensive student information including progress and performance
 */
@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val classUseCases: ClassUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<StudentDetailState, StudentDetailEvent>(
    StudentDetailState(),
    notificationManager
) {

    override fun onEvent(event: StudentDetailEvent) {
        when (event) {
            is StudentDetailEvent.LoadStudentDetail -> loadStudentDetail(event.studentId, event.classId)
            is StudentDetailEvent.ChangeTab -> setState { copy(selectedTab = event.tabIndex) }
            is StudentDetailEvent.Refresh -> refreshStudentDetail()
            is StudentDetailEvent.ClearError -> setState { copy(error = null) }
        }
    }

    private fun loadStudentDetail(studentId: String, classId: String) {
        launch {
            setState { 
                copy(
                    isLoading = true, 
                    studentId = studentId, 
                    classId = classId
                ) 
            }

            // Load user information
            loadUserInfo(studentId)
            
            // Load enrollment information
            loadEnrollmentInfo(studentId, classId)
            
            // Load academic progress (this would need additional use cases)
            // For now, we'll use placeholder data
            loadAcademicProgress(classId, studentId)
            
            setState { copy(isLoading = false) }
        }
    }

    private suspend fun loadUserInfo(studentId: String) {
        try {
            var userResult: Resource<com.example.datn.domain.models.User?>? = null

            userUseCases.getStudentUser(studentId).collect { result ->
                when (result) {
                    is Resource.Loading -> { /* Skip */ }
                    is Resource.Success -> {
                        userResult = result
                        return@collect
                    }
                    is Resource.Error -> {
                        userResult = result
                        return@collect
                    }
                    else -> { /* Skip */ }
                }
            }

            when (userResult) {
                is Resource.Success -> {
                    val user = (userResult as Resource.Success<com.example.datn.domain.models.User?>).data
                    setState { copy(userInfo = user) }
                }
                is Resource.Error -> {
                    val errorMsg = (userResult as Resource.Error<com.example.datn.domain.models.User?>).message
                    setState { copy(error = "Không thể tải thông tin học sinh: $errorMsg") }
                }
                else -> { /* Skip */ }
            }
        } catch (e: Exception) {
            setState { copy(error = "Lỗi: ${e.message}") }
        }
    }

    private suspend fun loadEnrollmentInfo(studentId: String, classId: String) {
        try {
            classUseCases.getApprovedStudentsInClass(classId).collectLatest { result ->
                when (result) {
                    is Resource.Success -> {
                        val enrollment = result.data?.find { it.studentId == studentId }
                        if (enrollment != null) {
                            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val enrolledDate = try {
                                enrollment.enrolledDate?.let { 
                                    dateFormatter.format(Date(it.epochSecond * 1000))
                                } ?: "N/A"
                            } catch (e: Exception) {
                                "N/A"
                            }
                            
                            setState { 
                                copy(
                                    enrollmentInfo = enrollment,
                                    enrolledDate = enrolledDate
                                ) 
                            }
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(error = result.message) }
                    }
                    else -> { /* Skip */ }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentDetailVM", "Error loading enrollment info", e)
        }
    }

    private fun loadAcademicProgress(classId: String, studentId: String) {
        // TODO: Implement actual use cases to fetch:
        // - Lesson progress
        // - Test scores
        // - Assignment/MiniGame progress
        
        // For now, using placeholder data
        setState {
            copy(
                totalLessons = 10,
                completedLessons = 6,
                lessonProgress = 0.6f,
                totalTests = 5,
                completedTests = 3,
                averageScore = 75.5f,
                testResults = listOf(
                    TestResult(
                        testId = "1",
                        testTitle = "Bài kiểm tra 1",
                        score = 85f,
                        maxScore = 100f,
                        completedDate = "01/11/2024",
                        passed = true
                    ),
                    TestResult(
                        testId = "2",
                        testTitle = "Bài kiểm tra 2",
                        score = 70f,
                        maxScore = 100f,
                        completedDate = "05/11/2024",
                        passed = true
                    ),
                    TestResult(
                        testId = "3",
                        testTitle = "Bài kiểm tra 3",
                        score = 72f,
                        maxScore = 100f,
                        completedDate = "08/11/2024",
                        passed = true
                    )
                ),
                totalAssignments = 8,
                completedAssignments = 5,
                assignmentProgress = 0.625f
            )
        }
    }

    private fun refreshStudentDetail() {
        val studentId = state.value.studentId
        val classId = state.value.classId
        if (studentId.isNotEmpty() && classId.isNotEmpty()) {
            loadStudentDetail(studentId, classId)
        }
    }
}
