package com.example.datn.domain.usecase.auth

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class RegisterParams(
    val email: String,
    val password: String,
    val name: String,
    val role: String
)

class RegisterUseCase @Inject constructor(
    private val repository: IAuthRepository
) : BaseUseCase<RegisterParams, Flow<Resource<User>>> {
    override fun invoke(params: RegisterParams): Flow<Resource<User>> {
        return repository.register(params.email, params.password, params.name, params.role)
    }
}
