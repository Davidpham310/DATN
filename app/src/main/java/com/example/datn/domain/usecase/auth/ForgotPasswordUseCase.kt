package com.example.datn.domain.usecase.auth

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ForgotPasswordParams(val email: String)

class ForgotPasswordUseCase @Inject constructor(
    private val repository: IAuthRepository
) : BaseUseCase<ForgotPasswordParams, Flow<Resource<String>>> {
    override fun invoke(params: ForgotPasswordParams): Flow<Resource<String>> {
        return repository.forgotPassword(params.email)
    }
}
