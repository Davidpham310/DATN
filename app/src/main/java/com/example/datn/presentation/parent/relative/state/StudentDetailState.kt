package com.example.datn.presentation.parent.relative.state

import com.example.datn.domain.models.StudyTimeStatistics
import com.example.datn.domain.usecase.parentstudent.LinkedStudentInfo
import com.example.datn.domain.usecase.progress.StudentDashboard
import com.example.datn.domain.usecase.progress.StudentLessonProgressItem

data class StudentDetailState(
    val isLoading: Boolean = false,
    val studentInfo: LinkedStudentInfo? = null,
    val dashboard: StudentDashboard? = null,
    val studyTime: StudyTimeStatistics? = null,
    val lessonProgressItems: List<StudentLessonProgressItem> = emptyList(),
    val error: String? = null,
    val isResettingPassword: Boolean = false,
    val resetPasswordSuccess: String? = null,
    val resetPasswordError: String? = null
)
