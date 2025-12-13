package com.example.datn.presentation.student.home.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.usecase.progress.StudentDashboard
import com.example.datn.domain.models.StudyTimeStatistics

data class StudentDashboardState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val dashboard: StudentDashboard? = null,
    val studyTime: StudyTimeStatistics? = null,
    val isRefreshing: Boolean = false,
    val studentId: String? = null
) : BaseState
