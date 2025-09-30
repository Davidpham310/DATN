package com.example.datn.domain.usecase.auth

import javax.inject.Inject

data class AuthUseCases @Inject constructor(
    val login: LoginUseCase,
    val register: RegisterUseCase,
    val forgotPassword: ForgotPasswordUseCase
)