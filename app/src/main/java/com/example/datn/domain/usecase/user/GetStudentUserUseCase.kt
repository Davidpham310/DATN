package com.example.datn.domain.usecase.user

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.User
import com.example.datn.domain.repository.IStudentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get User information from Student ID
 * This internally fetches Student record and then retrieves the associated User
 */
class GetStudentUserUseCase @Inject constructor(
    private val studentRepository: IStudentRepository
) {
    operator fun invoke(studentId: String): Flow<Resource<User?>> {
        return studentRepository.getStudentUser(studentId)
    }
}
