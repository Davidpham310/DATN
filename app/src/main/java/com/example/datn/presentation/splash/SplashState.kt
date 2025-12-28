package com.example.datn.presentation.splash

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.User

data class SplashState(
    override val isLoading: Boolean = true,
    val user: User? = null,
    override val error: String? = null
) : BaseState