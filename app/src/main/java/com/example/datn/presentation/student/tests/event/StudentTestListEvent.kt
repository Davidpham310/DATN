package com.example.datn.presentation.student.tests.event

import com.example.datn.core.base.BaseEvent

sealed class StudentTestListEvent : BaseEvent {
    object LoadTests : StudentTestListEvent()
    object RefreshTests : StudentTestListEvent()
    data class RequestStartTest(val testId: String) : StudentTestListEvent()
    data class NavigateToTest(val testId: String) : StudentTestListEvent()
    data class NavigateToResult(val testId: String, val resultId: String) : StudentTestListEvent()
}
