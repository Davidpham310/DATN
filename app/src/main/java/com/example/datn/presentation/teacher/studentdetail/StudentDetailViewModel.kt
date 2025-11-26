package com.example.datn.presentation.teacher.studentdetail

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.classmanager.ClassUseCases
import com.example.datn.domain.usecase.user.UserUseCases
import com.example.datn.domain.usecase.progress.GetStudentClassProgressUseCase
import com.example.datn.domain.usecase.progress.GetStudentClassLessonProgressUseCase
import com.example.datn.domain.usecase.progress.GetStudentAllLessonProgressUseCase
import com.example.datn.domain.usecase.progress.GetStudentDashboardUseCase
import com.example.datn.domain.usecase.progress.GetStudyTimeStatisticsUseCase
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
 * 
 * Can be used by:
 * - Teacher: classId provided, shows progress for specific class
 * - Parent: classId empty, shows progress across all classes
 */
@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val classUseCases: ClassUseCases,
    private val getStudentClassProgress: GetStudentClassProgressUseCase,
    private val getStudentClassLessonProgress: GetStudentClassLessonProgressUseCase,
    private val getStudentAllLessonProgress: GetStudentAllLessonProgressUseCase,
    private val getStudentDashboard: GetStudentDashboardUseCase,
    private val getStudyTimeStatistics: GetStudyTimeStatisticsUseCase,
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
            if (classId.isNotEmpty()) {
                loadEnrollmentInfo(studentId, classId)
            }
            
            // Load academic progress
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
                    Log.d("StudentDetailVM", "Loaded user info: $user")
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
                        Log.d("StudentDetailVM", "Loaded enrollment info: $enrollment")
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

    private suspend fun loadAcademicProgress(classId: String, studentId: String) {
        try {
            // Load dashboard data (works for both teacher and parent view)
            getStudentDashboard(studentId).collectLatest { dashboardResult ->
                when (dashboardResult) {
                    is Resource.Loading -> {
                        // Giữ nguyên state loading
                    }
                    is Resource.Success -> {
                        val dashboard = dashboardResult.data
                        Log.d("StudentDetailVM", "Loaded dashboard: $dashboard")
                        if (dashboard != null) {
                            val overview = dashboard.overview
                            setState {
                                copy(
                                    totalLessons = overview.totalLessons,
                                    completedLessons = overview.completedLessons,
                                    lessonProgress = if (overview.totalLessons > 0) {
                                        overview.averageLessonProgressPercent / 100f
                                    } else {
                                        0f
                                    },
                                    totalTests = overview.totalTests,
                                    completedTests = overview.completedTests,
                                    averageScore = overview.averageTestScorePercent?.toFloat() ?: 0f,
                                    totalStudyTimeSeconds = overview.totalStudyTimeSeconds,
                                    totalMiniGamesPlayed = overview.totalMiniGamesPlayed,
                                    averageMiniGameScorePercent = overview.averageMiniGameScorePercent?.toFloat()
                                        ?: 0f
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(error = dashboardResult.message) }
                    }
                }
            }
            
            // Load study time statistics
            getStudyTimeStatistics(studentId).collectLatest { timeResult ->
                when (timeResult) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        Log.d("StudentDetailVM", "Loaded study time statistics: $timeResult")
                    }
                    is Resource.Error -> {}
                }
            }
            
            if (classId.isNotEmpty()) {
                // Teacher view: Also load progress for specific class
                getStudentClassLessonProgress(studentId, classId).collectLatest { result ->
                    when (result) {
                        is Resource.Loading -> {}
                        is Resource.Success -> {
                            val items = result.data ?: emptyList()
                            setState { copy(lessonProgressItems = items) }
                        }
                        is Resource.Error -> {
                            setState { copy(error = result.message) }
                        }
                    }
                }
            } else {
                // Parent view: Load progress across all classes
                getStudentAllLessonProgress(studentId).collectLatest { result ->
                    when (result) {
                        is Resource.Loading -> {}
                        is Resource.Success -> {
                            val items = result.data ?: emptyList()
                            setState { copy(lessonProgressItems = items) }
                        }
                        is Resource.Error -> {
                            setState { copy(error = result.message ?: "Lỗi tải tiến độ học tập") }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            setState { copy(error = e.message ?: "Lỗi tải tiến độ học tập") }
        }
    }

    private fun refreshStudentDetail() {
        val studentId = state.value.studentId
        val classId = state.value.classId
        if (studentId.isNotEmpty()) {
            loadStudentDetail(studentId, classId)
        }
    }
}
