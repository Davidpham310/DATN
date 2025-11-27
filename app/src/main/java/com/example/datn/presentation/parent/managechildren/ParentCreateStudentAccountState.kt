package com.example.datn.presentation.parent.managechildren

import com.example.datn.core.base.BaseState

data class ParentCreateStudentAccountState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isSuccess: Boolean = false
) : BaseState
