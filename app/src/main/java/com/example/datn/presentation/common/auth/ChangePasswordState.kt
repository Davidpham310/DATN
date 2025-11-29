package com.example.datn.presentation.common.auth

import com.example.datn.core.base.BaseState

data class ChangePasswordState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val successMessage: String? = null
) : BaseState
