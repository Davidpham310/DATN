package com.example.datn.presentation.common.test

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestOption
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState

data class TestOptionState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val options: List<TestOption> = emptyList(),
    val selectedOption: TestOption? = null,
    val showAddEditDialog: Boolean = false,
    val editingOption: TestOption? = null,
    val confirmDeleteState: ConfirmationDialogState<TestOption> = ConfirmationDialogState.empty(),
    val currentQuestionId: String = "",
    val currentQuestionType: QuestionType? = null
) : BaseState


