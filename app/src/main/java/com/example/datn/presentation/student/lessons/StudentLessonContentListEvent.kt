package com.example.datn.presentation.student.lessons

import com.example.datn.core.base.BaseEvent

sealed class StudentLessonContentListEvent : BaseEvent {
    data class LoadLesson(val lessonId: String) : StudentLessonContentListEvent()
}
