package com.example.datn.presentation.student.tests

import com.example.datn.core.base.BaseEvent

sealed class StudentTestTakingEvent : BaseEvent {
    data class LoadTest(val testId: String) : StudentTestTakingEvent()
    object NextQuestion : StudentTestTakingEvent()
    object PreviousQuestion : StudentTestTakingEvent()
    data class GoToQuestion(val index: Int) : StudentTestTakingEvent()
    data class AnswerSingleChoice(val questionId: String, val optionId: String) : StudentTestTakingEvent()
    data class AnswerMultipleChoice(val questionId: String, val optionIds: Set<String>) : StudentTestTakingEvent()
    data class AnswerFillBlank(val questionId: String, val text: String) : StudentTestTakingEvent()
    data class AnswerEssay(val questionId: String, val text: String) : StudentTestTakingEvent()
    object ShowSubmitDialog : StudentTestTakingEvent()
    object DismissSubmitDialog : StudentTestTakingEvent()
    object ConfirmSubmit : StudentTestTakingEvent()
    object ToggleQuestionList : StudentTestTakingEvent()
    object SaveProgress : StudentTestTakingEvent()
}
