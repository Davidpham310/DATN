package com.example.datn.presentation.parent.relative.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.extensions.formatAsDateTime
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.auth.ForgotPasswordParams
import com.example.datn.domain.usecase.parentstudent.ParentStudentUseCases
import com.example.datn.domain.usecase.progress.GetStudentDashboardUseCase
import com.example.datn.domain.usecase.progress.GetStudentPerformanceDetailsUseCase
import com.example.datn.domain.usecase.progress.GetStudyTimeStatisticsUseCase
import com.example.datn.domain.usecase.progress.GetStudentAllLessonProgressUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.presentation.parent.relative.event.StudentDetailEvent
import com.example.datn.presentation.parent.relative.state.StudentDetailState
import com.example.datn.presentation.parent.relative.state.TestResult
import com.example.datn.presentation.parent.relative.state.MiniGameResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject

private const val TAG = "ParentStudentDetailVM"

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val parentStudentUseCases: ParentStudentUseCases,
    private val authUseCases: AuthUseCases,
    private val getStudentDashboard: GetStudentDashboardUseCase,
    private val getStudentPerformanceDetails: GetStudentPerformanceDetailsUseCase,
    private val getStudyTimeStatistics: GetStudyTimeStatisticsUseCase,
    private val getStudentAllLessonProgress: GetStudentAllLessonProgressUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentDetailState, StudentDetailEvent>(
    StudentDetailState(),
    notificationManager
) {
    
    // Giống như ClassManagerViewModel - tạo StateFlow cho parentId
    private val currentParentIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    override fun onEvent(event: StudentDetailEvent) {
        Log.d(TAG, "onEvent=$event")
        when (event) {
            is StudentDetailEvent.LoadStudentDetail -> loadStudentDetailInternal(event.studentId)
            is StudentDetailEvent.ChangeTab -> setState { copy(selectedTab = event.tabIndex) }
            StudentDetailEvent.ClearError -> setState { copy(error = null) }
            StudentDetailEvent.ClearMessages -> setState { copy(resetPasswordError = null, resetPasswordSuccess = null) }
        }
    }

    fun onTabSelected(tabIndex: Int) {
        Log.d(TAG, "onTabSelected tabIndex=$tabIndex")
        onEvent(StudentDetailEvent.ChangeTab(tabIndex))
    }

    fun loadStudentDetail(studentId: String) {
        onEvent(StudentDetailEvent.LoadStudentDetail(studentId))
    }

    private fun loadStudentDetailInternal(studentId: String) {
        Log.d(TAG, "loadStudentDetail: start studentId=$studentId")
        launch {
            setState { copy(isLoading = true, error = null) }

            // Lấy parent ID từ StateFlow (đợi giá trị hợp lệ)
            var parentId = currentParentIdFlow.value
            if (parentId.isBlank()) {
                Log.d(TAG, "loadStudentDetail: waiting for parentId...")
                currentParentIdFlow
                    .filter { it.isNotBlank() }
                    .take(1)
                    .collect { id ->
                        parentId = id
                    }
            }
            Log.d(TAG, "loadStudentDetail: parentId=$parentId")

            parentStudentUseCases.getLinkedStudents(parentId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "LinkedStudents: Loading parentId=$parentId")
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        Log.d(
                            TAG,
                            "LinkedStudents: Success parentId=$parentId count=${resource.data?.size ?: 0}"
                        )
                        val studentInfo = resource.data?.find { it.student.id == studentId }
                        if (studentInfo != null) {
                            Log.d(
                                TAG,
                                "LinkedStudents: Found studentId=$studentId name=${studentInfo.user.name} email=${studentInfo.user.email}"
                            )
                            setState {
                                copy(
                                    isLoading = false,
                                    studentInfo = studentInfo,
                                    error = null
                                )
                            }

                            Log.d(TAG, "loadStudentDetail: loadProgress(studentId=$studentId)")
                            loadProgress(studentId)
                        } else {
                            Log.w(TAG, "LinkedStudents: studentId=$studentId not found in linked list")
                            setState {
                                copy(
                                    isLoading = false,
                                    error = "Không tìm thấy thông tin học sinh"
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        Log.e(
                            TAG,
                            "LinkedStudents: Error parentId=$parentId msg=${resource.message}"
                        )
                        setState {
                            copy(
                                isLoading = false,
                                error = resource.message ?: "Lỗi tải thông tin học sinh"
                            )
                        }
                    }
                }
            }

            getStudentAllLessonProgress(studentId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "LessonProgress(all): Loading studentId=$studentId")
                    }
                    is Resource.Success -> {
                        Log.d(
                            TAG,
                            "LessonProgress(all): Success studentId=$studentId items=${result.data?.size ?: 0}"
                        )
                        setState { copy(lessonProgressItems = result.data ?: emptyList()) }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "LessonProgress(all): Error studentId=$studentId msg=${result.message}")
                    }
                }
            }
        }
    }

    private fun loadProgress(studentId: String) {
        launch {
            getStudentDashboard(studentId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "Dashboard: Loading studentId=$studentId")
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val overview = result.data?.overview
                        Log.d(TAG, "Dashboard: Success studentId=$studentId overview=$overview")
                        setState {
                            copy(
                                isLoading = false,
                                dashboard = result.data,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Dashboard: Error studentId=$studentId msg=${result.message}")
                        setState {
                            copy(
                                isLoading = false,
                                error = result.message ?: "Lỗi tải tiến độ học tập"
                            )
                        }
                    }
                }
            }

            getStudyTimeStatistics(studentId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "StudyTimeStatistics: Loading studentId=$studentId")
                    }
                    is Resource.Success -> {
                        val stats = result.data
                        Log.d(TAG, "StudyTimeStatistics: Success studentId=$studentId stats=$stats")
                        setState { copy(studyTime = stats) }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "StudyTimeStatistics: Error studentId=$studentId msg=${result.message}")
                    }
                }
            }

            getStudentPerformanceDetails(studentId).collect { detailsRes ->
                when (detailsRes) {
                    is Resource.Loading -> {
                        Log.d(TAG, "PerformanceDetails: Loading studentId=$studentId")
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
                            "PerformanceDetails: Success studentId=$studentId tests=${mappedTests.size} miniGames=${mappedMiniGames.size}"
                        )
                        setState { copy(testResults = mappedTests, miniGameResults = mappedMiniGames) }
                    }
                    is Resource.Error -> {
                        Log.e(
                            TAG,
                            "PerformanceDetails: Error studentId=$studentId msg=${detailsRes.message}"
                        )
                    }
                }
            }
        }
    }

    fun resetStudentPassword() {
        val currentState = state.value
        val studentEmail = currentState.studentInfo?.user?.email
        if (studentEmail.isNullOrBlank()) {
            setState {
                copy(
                    resetPasswordError = "Không tìm thấy email của học sinh",
                    resetPasswordSuccess = null
                )
            }
            return
        }

        launch {
            authUseCases.forgotPassword(ForgotPasswordParams(studentEmail)).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        setState {
                            copy(
                                isResettingPassword = true,
                                resetPasswordError = null,
                                resetPasswordSuccess = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        setState {
                            copy(
                                isResettingPassword = false,
                                resetPasswordError = null,
                                resetPasswordSuccess = result.data
                                    ?: "Đã gửi email đổi mật khẩu cho học sinh"
                            )
                        }
                        showNotification(
                            result.data ?: "Đã gửi email đổi mật khẩu cho học sinh",
                            NotificationType.SUCCESS
                        )
                    }
                    is Resource.Error -> {
                        setState {
                            copy(
                                isResettingPassword = false,
                                resetPasswordSuccess = null,
                                resetPasswordError = result.message
                                    ?: "Lỗi gửi email đổi mật khẩu"
                            )
                        }
                    }
                }
            }
        }
    }
}
