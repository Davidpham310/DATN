package com.example.datn.domain.usecase.auth

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ChangePasswordParams(
    val currentPassword: String,
    val newPassword: String
)

class ChangePasswordUseCase @Inject constructor(
    private val repository: IAuthRepository
) : BaseUseCase<ChangePasswordParams, Flow<Resource<Unit>>> {
    override fun invoke(params: ChangePasswordParams): Flow<Resource<Unit>> {
        return repository.changePassword(params.currentPassword, params.newPassword)
    }
}
