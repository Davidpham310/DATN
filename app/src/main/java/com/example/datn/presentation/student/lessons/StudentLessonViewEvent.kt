package com.example.datn.presentation.student.lessons

import com.example.datn.core.base.BaseEvent

sealed class StudentLessonViewEvent : BaseEvent {
    data class LoadLesson(val lessonId: String) : StudentLessonViewEvent()
    object NextContent : StudentLessonViewEvent()
    object PreviousContent : StudentLessonViewEvent()
    data class GoToContent(val index: Int) : StudentLessonViewEvent()
    object MarkCurrentAsViewed : StudentLessonViewEvent()
    object ShowProgressDialog : StudentLessonViewEvent()
    object DismissProgressDialog : StudentLessonViewEvent()
    object SaveProgress : StudentLessonViewEvent()
}
