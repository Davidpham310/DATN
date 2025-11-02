package com.example.datn.presentation.common.account

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.User

data class AccountState(
    val currentUser: User? = null,
    val isSigningOut: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val successMessage: String? = null
) : BaseState
