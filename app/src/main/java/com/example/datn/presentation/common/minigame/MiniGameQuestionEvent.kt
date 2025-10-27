package com.example.datn.presentation.common.minigame

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.QuestionType

sealed class MiniGameQuestionEvent : BaseEvent {
    object RefreshQuestions : MiniGameQuestionEvent()
    data class LoadQuestionsForGame(val gameId: String) : MiniGameQuestionEvent()
    data class SelectQuestion(val question: MiniGameQuestion) : MiniGameQuestionEvent()

    // Dialog actions
    object ShowAddQuestionDialog : MiniGameQuestionEvent()
    data class EditQuestion(val question: MiniGameQuestion) : MiniGameQuestionEvent()
    data class DeleteQuestion(val question: MiniGameQuestion) : MiniGameQuestionEvent()
    object DismissDialog : MiniGameQuestionEvent()

    // CRUD operations
    data class ConfirmAddQuestion(
        val gameId: String,
        val content: String,
        val questionType: QuestionType,
        val score: Double,
        val timeLimit: Long
    ) : MiniGameQuestionEvent()

    data class ConfirmEditQuestion(
        val id: String,
        val gameId: String,
        val content: String,
        val questionType: QuestionType,
        val score: Double,
        val timeLimit: Long
    ) : MiniGameQuestionEvent()
}