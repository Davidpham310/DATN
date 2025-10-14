package com.example.datn.presentation.common.user

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.User

data class UserState(
    val user: User? = null,
    val users: List<User> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null,
) : BaseState