package com.example.datn.domain.usecase.user

import javax.inject.Inject

data class UserUseCases @Inject constructor(
    val getUserById: GetUserByIdUseCase,
    val getAllUsers: GetAllUsersUseCase,
    val addUser: AddUserUseCase,
    val updateUserProfile: UpdateUserProfileUseCase,
    val deleteUser: DeleteUserUseCase,
    val updateAvatar: UpdateAvatarUseCase,
    val getStudentUser: GetStudentUserUseCase
)