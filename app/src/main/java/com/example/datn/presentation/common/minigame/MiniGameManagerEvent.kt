package com.example.datn.presentation.common.minigame

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.Level
import com.example.datn.domain.models.MiniGame

sealed class MiniGameManagerEvent : BaseEvent {
    object RefreshGames : MiniGameManagerEvent()
    data class LoadGamesForLesson(val lessonId: String) : MiniGameManagerEvent()
    data class SelectGame(val game: MiniGame) : MiniGameManagerEvent()

    // Dialog actions
    object ShowAddGameDialog : MiniGameManagerEvent()
    data class EditGame(val game: MiniGame) : MiniGameManagerEvent()
    data class DeleteGame(val game: MiniGame) : MiniGameManagerEvent()
    object DismissDialog : MiniGameManagerEvent()

    // CRUD operations
    data class ConfirmAddGame(
        val lessonId: String,
        val title: String,
        val description: String,
        val level: Level
    ) : MiniGameManagerEvent()

    data class ConfirmEditGame(
        val id: String,
        val lessonId: String,
        val title: String,
        val description: String,
        val level: Level
    ) : MiniGameManagerEvent()
}