package com.example.datn.presentation.student.tests.event

import com.example.datn.core.base.BaseEvent

sealed class StudentTestResultEvent : BaseEvent {
    data class LoadResult(val testId: String, val resultId: String) : StudentTestResultEvent()
    object ToggleDetailedAnswers : StudentTestResultEvent()
    object NavigateBack : StudentTestResultEvent()
}
