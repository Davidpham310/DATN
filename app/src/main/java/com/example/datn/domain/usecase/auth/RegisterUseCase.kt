package com.example.datn.domain.usecase.auth

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.models.UserRole
import com.example.datn.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

data class RegisterParams(
    val email: String,
    val password: String,
    val name: String,
    val role: UserRole
)

class RegisterUseCase @Inject constructor(
    private val repository: IAuthRepository
) : BaseUseCase<RegisterParams, Flow<Resource<User>>> {
    override fun invoke(params: RegisterParams): Flow<Resource<User>> {
        val now = Instant.now()
        val user = User(
            id = "",
            name = params.name,
            email = params.email,
            role = params.role,
            avatarUrl = null,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        return repository.register(user, params.password)
    }
}
