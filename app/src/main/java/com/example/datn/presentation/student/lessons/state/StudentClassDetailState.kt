package com.example.datn.presentation.student.lessons.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.StudentLessonProgress
import com.example.datn.domain.usecase.lesson.LessonWithStatus

data class StudentClassDetailState(
    val classInfo: Class? = null,
    val lessons: List<LessonWithStatus> = emptyList(),
    val studentCount: Int = 0,
    val lessonProgress: Map<String, StudentLessonProgress> = emptyMap(),
    val lessonContentCounts: Map<String, Int> = emptyMap(),
    val showWithdrawDialog: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState
