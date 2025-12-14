package com.example.datn.presentation.parent.relative.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.StudyTimeStatistics
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.progress.StudentDashboard
import com.example.datn.domain.usecase.progress.StudentLessonProgressItem

data class StudentDetailState(
    override val isLoading: Boolean = false,
    val studentInfo: LinkedStudentInfo? = null,
    val dashboard: StudentDashboard? = null,
    val studyTime: StudyTimeStatistics? = null,
    val lessonProgressItems: List<StudentLessonProgressItem> = emptyList(),
    val testResults: List<TestResult> = emptyList(),
    val miniGameResults: List<MiniGameResult> = emptyList(),
    override val error: String? = null,
    val isResettingPassword: Boolean = false,
    val resetPasswordSuccess: String? = null,
    val resetPasswordError: String? = null,
    val selectedTab: Int = 0
) : BaseState

data class TestResult(
    val testId: String = "",
    val testTitle: String = "",
    val score: Float = 0f,
    val maxScore: Float = 100f,
    val durationSeconds: Long = 0L,
    val completedDate: String = "",
    val passed: Boolean = false
)

data class MiniGameResult(
    val miniGameId: String = "",
    val miniGameTitle: String = "",
    val score: Float = 0f,
    val maxScore: Float = 0f,
    val scorePercent: Float = 0f,
    val completedDate: String = "",
    val durationSeconds: Long = 0L,
    val attemptNumber: Int = 1
)
