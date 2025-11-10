package com.example.datn.presentation.student.games

import com.example.datn.core.base.BaseEvent

sealed class MiniGameResultEvent : BaseEvent {
    data class LoadResult(val miniGameId: String, val resultId: String) : MiniGameResultEvent()
    object ToggleDetailedAnswers : MiniGameResultEvent()
    object PlayAgain : MiniGameResultEvent()
}
