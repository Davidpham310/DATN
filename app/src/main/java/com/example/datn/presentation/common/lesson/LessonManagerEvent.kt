package com.example.datn.presentation.common.lesson

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent

// ==================== EVENTS ====================

sealed class LessonManagerEvent : BaseEvent {
    object RefreshLessons : LessonManagerEvent()
    data class LoadLessonsForClass(val classId: String) : LessonManagerEvent()
    data class SelectLesson(val lesson: Lesson) : LessonManagerEvent()

    // Dialog actions
    object ShowAddLessonDialog : LessonManagerEvent()
    data class EditLesson(val lesson: Lesson) : LessonManagerEvent()
    data class DeleteLesson(val lesson: Lesson) : LessonManagerEvent()
    object DismissDialog : LessonManagerEvent()

    // CRUD operations
    data class ConfirmAddLesson(
        val classId: String,
        val title: String,
        val description: String?
    ) : LessonManagerEvent()

    data class ConfirmEditLesson(
        val id: String,
        val classId: String,
        val title: String,
        val description: String?,
        val order: Int
    ) : LessonManagerEvent()
}
