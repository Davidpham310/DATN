package com.example.datn.presentation.student.games

import com.example.datn.core.base.BaseEvent

sealed class MiniGamePlayEvent : BaseEvent {
    data class LoadMiniGame(val miniGameId: String) : MiniGamePlayEvent()
    data class AnswerQuestion(val questionId: String, val answer: String) : MiniGamePlayEvent()
    data class ToggleMultipleChoice(val questionId: String, val optionId: String) : MiniGamePlayEvent()
    data class CreateMatchingPair(val questionId: String, val option1Id: String, val option2Id: String) : MiniGamePlayEvent()
    data class RemoveMatchingPair(val questionId: String, val optionId: String) : MiniGamePlayEvent()
    data class NavigateToQuestion(val questionIndex: Int) : MiniGamePlayEvent()
    object ShowSubmitDialog : MiniGamePlayEvent()
    object DismissSubmitDialog : MiniGamePlayEvent()
    object ConfirmSubmit : MiniGamePlayEvent()
    object TimerTick : MiniGamePlayEvent()
    object ResetGame : MiniGamePlayEvent()
}
