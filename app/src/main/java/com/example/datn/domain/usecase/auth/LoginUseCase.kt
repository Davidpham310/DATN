package com.example.datn.domain.usecase.auth

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class LoginParams(val email: String, val password: String)

class LoginUseCase @Inject constructor(
    private val repository: IAuthRepository
) : BaseUseCase<LoginParams, Flow<Resource<User>>> {
    override fun invoke(params: LoginParams): Flow<Resource<User>> {
        return repository.login(params.email, params.password)
    }
}
