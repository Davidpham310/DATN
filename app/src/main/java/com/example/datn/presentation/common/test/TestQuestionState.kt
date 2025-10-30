package com.example.datn.presentation.common.test

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestQuestion
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState

data class TestQuestionState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val questions: List<TestQuestion> = emptyList(),
    val selectedQuestion: TestQuestion? = null,
    val showAddEditDialog: Boolean = false,
    val editingQuestion: TestQuestion? = null,
    val confirmDeleteState: ConfirmationDialogState<TestQuestion> = ConfirmationDialogState.empty(),
    val currentTestId: String = ""
) : BaseState
