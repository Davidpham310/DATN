package com.example.datn.presentation.student.lessons.event

import com.example.datn.core.base.BaseEvent

sealed class StudentClassDetailEvent : BaseEvent {
    data class LoadClassDetail(val classId: String) : StudentClassDetailEvent()
    data class NavigateToLesson(val lessonId: String) : StudentClassDetailEvent()
    object ShowWithdrawDialog : StudentClassDetailEvent()
    object DismissWithdrawDialog : StudentClassDetailEvent()
    object ConfirmWithdraw : StudentClassDetailEvent()
}
