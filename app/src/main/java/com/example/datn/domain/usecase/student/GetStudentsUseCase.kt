package com.example.datn.domain.usecase.student

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStudentsUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    operator fun invoke(): Flow<Resource<List<User>>> {
        // This would fetch all students from the repository
        // For now, returning empty list as placeholder
        return userRepository.getAllUsers()
    }
}
