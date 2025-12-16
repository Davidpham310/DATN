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
import com.example.datn.domain.usecase.progress.GetStudentClassPerformanceDetailsUseCase
import com.example.datn.domain.usecase.progress.GetStudentDashboardUseCase
import com.example.datn.domain.usecase.progress.GetStudyTimeStatisticsUseCase
import com.example.datn.core.utils.extensions.formatAsDateTime
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
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
    private val getStudentClassPerformanceDetails: GetStudentClassPerformanceDetailsUseCase,
    private val getStudentDashboard: GetStudentDashboardUseCase,
    private val getStudyTimeStatistics: GetStudyTimeStatisticsUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentDetailState, StudentDetailEvent>(
    StudentDetailState(),
    notificationManager
) {

    private companion object {
        const val TAG = "TeacherStudentDetailVM"
    }

    private suspend fun <T> awaitFirstNonLoading(flow: Flow<Resource<T>>): Resource<T> {
        var result: Resource<T>? = null
        flow
            .filter { it !is Resource.Loading }
            .take(1)
            .collect { value -> result = value }
        return result ?: Resource.Error("Không thể tải dữ liệu")
    }

    override fun onEvent(event: StudentDetailEvent) {
        Log.d(TAG, "onEvent=$event")
        when (event) {
            is StudentDetailEvent.LoadStudentDetail -> loadStudentDetail(event.studentId, event.classId)
            is StudentDetailEvent.ChangeTab -> setState { copy(selectedTab = event.tabIndex) }
            is StudentDetailEvent.Refresh -> refreshStudentDetail()
            is StudentDetailEvent.ClearError -> setState { copy(error = null) }
        }
    }

    private fun loadStudentDetail(studentId: String, classId: String) {
        Log.d(TAG, "loadStudentDetail: start studentId=$studentId classId=$classId")
        launch {
            setState { 
                copy(
                    isLoading = true, 
                    studentId = studentId, 
                    classId = classId
                ) 
            }

            Log.d(TAG, "loadStudentDetail: loadUserInfo(studentId=$studentId)")

            // Load user information
            loadUserInfo(studentId)
            
            // Load enrollment information
            if (classId.isNotEmpty()) {
                Log.d(TAG, "loadStudentDetail: loadEnrollmentInfo(studentId=$studentId, classId=$classId)")
                loadEnrollmentInfo(studentId, classId)
            }
            
            // Load academic progress
            Log.d(TAG, "loadStudentDetail: loadAcademicProgress(studentId=$studentId, classId=$classId)")
            loadAcademicProgress(classId, studentId)
            
            setState { copy(isLoading = false) }
            Log.d(TAG, "loadStudentDetail: done studentId=$studentId classId=$classId")
        }
    }

    private suspend fun loadUserInfo(studentId: String) {
        try {
            var userResult: Resource<com.example.datn.domain.models.User?>? = null

            userUseCases.getStudentUser(studentId).collect { result ->
                when (result) {
                    is Resource.Loading -> { 
                        Log.d(TAG, "UserInfo: Loading studentId=$studentId")
                    }
                    is Resource.Success -> {
                        userResult = result
                        Log.d(TAG, "UserInfo: Success studentId=$studentId user=${result.data}")
                        return@collect
                    }
                    is Resource.Error -> {
                        userResult = result
                        Log.e(TAG, "UserInfo: Error studentId=$studentId msg=${result.message}")
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
                    is Resource.Loading -> {
                        Log.d(TAG, "EnrollmentInfo: Loading studentId=$studentId classId=$classId")
                    }
                    is Resource.Success -> {
                        val enrollment = result.data?.find { it.studentId == studentId }
                        Log.d(TAG, "EnrollmentInfo: Success studentId=$studentId classId=$classId enrollment=$enrollment")
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
                        Log.e(TAG, "EnrollmentInfo: Error studentId=$studentId classId=$classId msg=${result.message}")
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
        launch {
            try {
                getStudentDashboard(studentId).collectLatest { dashboardResult ->
                    when (dashboardResult) {
                        is Resource.Loading -> {
                            Log.d(TAG, "Dashboard: Loading studentId=$studentId")
                            // Giữ nguyên state loading
                        }
                        is Resource.Success -> {
                            val dashboard = dashboardResult.data
                            Log.d(TAG, "Dashboard: Success studentId=$studentId overview=${dashboard?.overview}")
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
                            Log.e(TAG, "Dashboard: Error studentId=$studentId msg=${dashboardResult.message}")
                            setState { copy(error = dashboardResult.message) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Dashboard: Exception studentId=$studentId", e)
                setState { copy(error = e.message ?: "Lỗi tải tổng quan") }
            }
        }

        launch {
            try {
                Log.d(TAG, "StudyTimeStatistics: start studentId=$studentId")
                val timeResult = awaitFirstNonLoading(getStudyTimeStatistics(studentId))
                when (timeResult) {
                    is Resource.Success -> {
                        Log.d(
                            TAG,
                            "StudyTimeStatistics: Success studentId=$studentId stats=${timeResult.data}"
                        )
                        setState { copy(studyTimeStatistics = timeResult.data) }
                    }
                    is Resource.Error -> {
                        Log.w(TAG, "StudyTimeStatistics: Error studentId=$studentId msg=${timeResult.message}")
                    }
                    else -> {
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "StudyTimeStatistics: Exception studentId=$studentId msg=${e.message}")
            }
        }

        if (classId.isNotEmpty()) {
            launch {
                try {
                    Log.d(TAG, "LessonProgress(class): start studentId=$studentId classId=$classId")
                    getStudentClassLessonProgress(studentId, classId).collectLatest { result ->
                        when (result) {
                            is Resource.Loading -> {
                                Log.d(TAG, "LessonProgress(class): Loading studentId=$studentId classId=$classId")
                            }
                            is Resource.Success -> {
                                val items = result.data ?: emptyList()
                                Log.d(
                                    TAG,
                                    "LessonProgress(class): Success studentId=$studentId classId=$classId items=${items.size}"
                                )
                                setState { copy(lessonProgressItems = items) }
                            }
                            is Resource.Error -> {
                                Log.e(
                                    TAG,
                                    "LessonProgress(class): Error studentId=$studentId classId=$classId msg=${result.message}"
                                )
                                setState { copy(error = result.message) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "LessonProgress(class): Exception studentId=$studentId classId=$classId", e)
                    setState { copy(error = e.message ?: "Lỗi tải tiến độ bài học") }
                }
            }

            launch {
                try {
                    Log.d(TAG, "ClassPerformanceDetails: start studentId=$studentId classId=$classId")
                    getStudentClassPerformanceDetails(studentId, classId).collectLatest { detailsRes ->
                        when (detailsRes) {
                            is Resource.Loading -> {
                                Log.d(TAG, "ClassPerformanceDetails: Loading studentId=$studentId classId=$classId")
                            }
                            is Resource.Success -> {
                                val details = detailsRes.data
                                val mappedTests = details?.testResults.orEmpty().map { item ->
                                    val scorePercent = if (item.maxScore > 0) {
                                        (item.score * 100.0) / item.maxScore
                                    } else {
                                        0.0
                                    }
                                    TestResult(
                                        testId = item.testId,
                                        testTitle = item.testTitle,
                                        score = item.score.toFloat(),
                                        maxScore = item.maxScore.toFloat(),
                                        durationSeconds = item.durationSeconds,
                                        completedDate = item.submissionTime.formatAsDateTime(),
                                        passed = scorePercent >= 50.0
                                    )
                                }
                                val mappedMiniGames = details?.miniGameResults.orEmpty().map { item ->
                                    val scorePercent = if (item.maxScore > 0) {
                                        (item.score * 100.0) / item.maxScore
                                    } else {
                                        0.0
                                    }
                                    MiniGameResult(
                                        miniGameId = item.miniGameId,
                                        miniGameTitle = item.miniGameTitle,
                                        score = item.score.toFloat(),
                                        maxScore = item.maxScore.toFloat(),
                                        scorePercent = scorePercent.toFloat(),
                                        completedDate = item.submissionTime.formatAsDateTime(),
                                        durationSeconds = item.durationSeconds,
                                        attemptNumber = item.attemptNumber
                                    )
                                }
                                Log.d(
                                    TAG,
                                    "ClassPerformanceDetails: Success studentId=$studentId classId=$classId tests=${mappedTests.size} miniGames=${mappedMiniGames.size}"
                                )
                                setState {
                                    copy(
                                        testResults = mappedTests,
                                        miniGameResults = mappedMiniGames
                                    )
                                }
                            }
                            is Resource.Error -> {
                                Log.w(
                                    TAG,
                                    "ClassPerformanceDetails: Error studentId=$studentId classId=$classId msg=${detailsRes.message}"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "ClassPerformanceDetails: Exception studentId=$studentId classId=$classId msg=${e.message}"
                    )
                }
            }
        } else {
            launch {
                try {
                    Log.d(TAG, "LessonProgress(all): start studentId=$studentId")
                    getStudentAllLessonProgress(studentId).collectLatest { result ->
                        when (result) {
                            is Resource.Loading -> {
                                Log.d(TAG, "LessonProgress(all): Loading studentId=$studentId")
                            }
                            is Resource.Success -> {
                                val items = result.data ?: emptyList()
                                Log.d(TAG, "LessonProgress(all): Success studentId=$studentId items=${items.size}")
                                setState { copy(lessonProgressItems = items) }
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "LessonProgress(all): Error studentId=$studentId msg=${result.message}")
                                setState { copy(error = result.message ?: "Lỗi tải tiến độ học tập") }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "LessonProgress(all): Exception studentId=$studentId", e)
                    setState { copy(error = e.message ?: "Lỗi tải tiến độ học tập") }
                }
            }
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
