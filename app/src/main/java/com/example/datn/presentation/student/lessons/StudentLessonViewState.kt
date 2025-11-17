package com.example.datn.presentation.student.lessons

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.models.StudentLessonProgress

data class StudentLessonViewState(
    val lesson: Lesson? = null,
    val lessonContents: List<LessonContent> = emptyList(),
    val progress: StudentLessonProgress? = null,
    val lessonId: String? = null,
    val studentId: String? = null,
    val currentContentIndex: Int = 0,
    val viewedContentIds: Set<String> = emptySet(),
    val sessionStartTime: Long = 0L,
    val showProgressDialog: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState {
    val currentContent: LessonContent?
        get() = lessonContents.getOrNull(currentContentIndex)
    
    val canGoPrevious: Boolean
        get() = currentContentIndex > 0
    
    val canGoNext: Boolean
        get() = currentContentIndex < lessonContents.size - 1
    
    val progressPercentage: Int
        get() = if (lessonContents.isEmpty()) 0
               else (viewedContentIds.size * 100) / lessonContents.size
    
    val isLessonCompleted: Boolean
        get() = progressPercentage == 100
}
