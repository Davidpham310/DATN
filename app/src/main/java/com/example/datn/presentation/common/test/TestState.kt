package com.example.datn.presentation.common.test

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Test
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState

data class TestState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val tests: List<Test> = emptyList(),
    val selectedTest: Test? = null,
    val showAddEditDialog: Boolean = false,
    val editingTest: Test? = null,
    val confirmDeleteState: ConfirmationDialogState<Test> = ConfirmationDialogState.empty()
) : BaseState
