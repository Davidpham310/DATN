package com.example.datn.domain.usecase.user

import com.example.datn.domain.repository.IUserRepository
import javax.inject.Inject

class GetAllUsersUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    operator fun invoke() = userRepository.getAllUsers()
}