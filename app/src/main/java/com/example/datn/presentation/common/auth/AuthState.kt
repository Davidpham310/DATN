package com.example.datn.presentation.common.auth

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.User

data class AuthState(
    val user: User? = null,
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val navigateTo: String? = null
) : BaseState