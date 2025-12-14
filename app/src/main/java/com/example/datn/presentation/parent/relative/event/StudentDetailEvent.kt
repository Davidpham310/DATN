package com.example.datn.presentation.parent.relative.event

import com.example.datn.core.base.BaseEvent

sealed class StudentDetailEvent : BaseEvent {
    data class LoadStudentDetail(val studentId: String) : StudentDetailEvent()

    data class ChangeTab(val tabIndex: Int) : StudentDetailEvent()

    data object ClearError : StudentDetailEvent()

    data object ClearMessages : StudentDetailEvent()
}
