package com.example.datn.presentation.student.home

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.progress.GetStudentDashboardUseCase
import com.example.datn.domain.usecase.progress.GetStudyTimeStatisticsUseCase
import com.example.datn.domain.usecase.student.GetStudentProfileByUserIdUseCase
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StudentDashboardViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    private val getStudentProfileByUserId: GetStudentProfileByUserIdUseCase,
    private val getStudentDashboard: GetStudentDashboardUseCase,
    private val getStudyTimeStatistics: GetStudyTimeStatisticsUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<StudentDashboardState, StudentDashboardEvent>(
    StudentDashboardState(),
    notificationManager
) {

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    init {
        loadDashboard()
    }

    override fun onEvent(event: StudentDashboardEvent) {
        when (event) {
            StudentDashboardEvent.LoadDashboard -> loadDashboard()
            StudentDashboardEvent.Refresh -> loadDashboard(isRefresh = true)
            StudentDashboardEvent.ClearError -> setState { copy(error = null) }
        }
    }

    private fun loadDashboard(isRefresh: Boolean = false) {
        launch {
            val loadingFlag = !isRefresh
            setState { copy(isLoading = loadingFlag, isRefreshing = isRefresh, error = null) }

            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.first { it.isNotBlank() }
            }

            if (currentUserId.isBlank()) {
                setState { copy(isLoading = false, isRefreshing = false) }
                showNotification("Vui lòng đăng nhập", NotificationType.ERROR)
                return@launch
            }

            getStudentProfileByUserId(currentUserId).collectLatest { profileResult ->
                when (profileResult) {
                    is Resource.Loading -> {
                        if (!isRefresh) {
                            setState { copy(isLoading = true) }
                        }
                    }
                    is Resource.Success -> {
                        val studentId = profileResult.data?.id
                        if (studentId.isNullOrBlank()) {
                            setState {
                                copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    error = "Không tìm thấy thông tin học sinh"
                                )
                            }
                            showNotification("Không tìm thấy thông tin học sinh", NotificationType.ERROR)
                            return@collectLatest
                        }

                        setState { copy(studentId = studentId) }
                        loadDashboardForStudent(studentId, isRefresh)
                    }
                    is Resource.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = profileResult.message
                            )
                        }
                        showNotification(profileResult.message ?: "Không thể tải dashboard", NotificationType.ERROR)
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun loadDashboardForStudent(studentId: String, isRefresh: Boolean) {
        getStudentDashboard(studentId).collectLatest { result ->
            when (result) {
                is Resource.Loading -> {
                    if (!isRefresh) {
                        setState { copy(isLoading = true) }
                    }
                }
                is Resource.Success -> {
                    setState {
                        copy(
                            dashboard = result.data,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    setState {
                        copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.message
                        )
                    }
                    showNotification(result.message ?: "Không thể tải dashboard", NotificationType.ERROR)
                    return@collectLatest
                }
                else -> {}
            }
        }

        getStudyTimeStatistics(studentId).collectLatest { result ->
            when (result) {
                is Resource.Loading -> {
                    // Optional: keep main loading state from dashboard
                }
                is Resource.Success -> {
                    setState {
                        copy(
                            studyTime = result.data,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    setState {
                        copy(
                            isRefreshing = false,
                            error = result.message
                        )
                    }
                    showNotification(
                        result.message ?: "Không thể tải thống kê thời gian học",
                        NotificationType.ERROR
                    )
                }
                else -> {}
            }
        }
    }
}
