package com.example.datn.presentation.common.minigame

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.MiniGameOption

sealed class MiniGameOptionEvent : BaseEvent {
    object RefreshOptions : MiniGameOptionEvent()
    data class LoadOptionsForQuestion(val questionId: String) : MiniGameOptionEvent()
    data class SelectOption(val option: MiniGameOption) : MiniGameOptionEvent()

    // Dialog actions
    object ShowAddOptionDialog : MiniGameOptionEvent()
    data class EditOption(val option: MiniGameOption) : MiniGameOptionEvent()
    data class DeleteOption(val option: MiniGameOption) : MiniGameOptionEvent()
    object DismissDialog : MiniGameOptionEvent()

    // CRUD operations
    data class ConfirmAddOption(
        val questionId: String,
        val content: String,
        val isCorrect: Boolean,
        val mediaUrl: String?
    ) : MiniGameOptionEvent()

    data class ConfirmEditOption(
        val id: String,
        val questionId: String,
        val content: String,
        val isCorrect: Boolean,
        val mediaUrl: String?
    ) : MiniGameOptionEvent()
}