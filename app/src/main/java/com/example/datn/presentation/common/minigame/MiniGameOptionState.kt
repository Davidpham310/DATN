package com.example.datn.presentation.common.minigame

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.GameType
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.QuestionType
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState

data class MiniGameOptionState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val options: List<MiniGameOption> = emptyList(),
    val selectedOption: MiniGameOption? = null,
    val showAddEditDialog: Boolean = false,
    val editingOption: MiniGameOption? = null,
    val confirmDeleteState: ConfirmationDialogState<MiniGameOption> = ConfirmationDialogState.empty(),
    val currentQuestionId: String = "",
    val currentQuestionType: QuestionType? = null,
    val currentGameType: GameType? = null
) : BaseState