package com.example.datn.presentation.common.minigame

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState

data class MiniGameQuestionState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val questions: List<MiniGameQuestion> = emptyList(),
    val selectedQuestion: MiniGameQuestion? = null,
    val questionOptions: Map<String, List<MiniGameOption>> = emptyMap(),
    val showAddEditDialog: Boolean = false,
    val editingQuestion: MiniGameQuestion? = null,
    val confirmDeleteState: ConfirmationDialogState<MiniGameQuestion> = ConfirmationDialogState.empty(),
    val currentGameId: String = "",
    val currentGame: MiniGame? = null
) : BaseState