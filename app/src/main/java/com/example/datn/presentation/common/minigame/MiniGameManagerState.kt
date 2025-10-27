package com.example.datn.presentation.common.minigame

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.MiniGame
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState

data class MiniGameManagerState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val miniGames: List<MiniGame> = emptyList(),
    val selectedGame: MiniGame? = null,
    val showAddEditDialog: Boolean = false,
    val editingGame: MiniGame? = null,
    val confirmDeleteState: ConfirmationDialogState<MiniGame> = ConfirmationDialogState.empty(),
    val currentLessonId: String = ""
) : BaseState