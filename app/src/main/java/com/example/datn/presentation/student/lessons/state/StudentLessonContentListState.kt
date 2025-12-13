package com.example.datn.presentation.student.lessons.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.usecase.lesson.LessonContentWithStatus

data class StudentLessonContentListState(
    val lesson: Lesson? = null,
    val contents: List<LessonContentWithStatus> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState
