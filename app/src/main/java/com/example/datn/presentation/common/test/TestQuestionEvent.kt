package com.example.datn.presentation.common.test

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestQuestion

sealed class TestQuestionEvent : BaseEvent {
    data class LoadQuestions(val testId: String) : TestQuestionEvent()
    object RefreshQuestions : TestQuestionEvent()
    data class SelectQuestion(val question: TestQuestion) : TestQuestionEvent()
    
    // Dialog events
    object ShowAddQuestionDialog : TestQuestionEvent()
    data class EditQuestion(val question: TestQuestion) : TestQuestionEvent()
    data class DeleteQuestion(val question: TestQuestion) : TestQuestionEvent()
    object DismissDialog : TestQuestionEvent()
    
    // CRUD events
    data class ConfirmAddQuestion(
        val testId: String,
        val content: String,
        val score: Double,
        val timeLimit: Int,
        val order: Int,
        val questionType: QuestionType,
        val mediaUrl: String?
    ) : TestQuestionEvent()
    
    data class ConfirmEditQuestion(
        val id: String,
        val testId: String,
        val content: String,
        val score: Double,
        val timeLimit: Int,
        val order: Int,
        val questionType: QuestionType,
        val mediaUrl: String?
    ) : TestQuestionEvent()
}
