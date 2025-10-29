package com.example.datn.presentation.common.test

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.TestOption

sealed class TestOptionEvent : BaseEvent {
    object RefreshOptions : TestOptionEvent()
    data class LoadOptionsForQuestion(val questionId: String) : TestOptionEvent()
    data class SelectOption(val option: TestOption) : TestOptionEvent()

    object ShowAddOptionDialog : TestOptionEvent()
    data class EditOption(val option: TestOption) : TestOptionEvent()
    data class DeleteOption(val option: TestOption) : TestOptionEvent()
    object DismissDialog : TestOptionEvent()

    data class ConfirmAddOption(
        val questionId: String,
        val content: String,
        val isCorrect: Boolean,
        val mediaUrl: String?
    ) : TestOptionEvent()

    data class ConfirmEditOption(
        val id: String,
        val questionId: String,
        val content: String,
        val isCorrect: Boolean,
        val mediaUrl: String?
    ) : TestOptionEvent()
}


