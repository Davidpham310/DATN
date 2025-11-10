package com.example.datn.domain.usecase.student

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Student
import com.example.datn.domain.repository.IStudentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStudentProfileByUserIdUseCase @Inject constructor(
    private val repository: IStudentRepository
) {
    operator fun invoke(userId: String): Flow<Resource<Student?>> {
        return repository.getStudentProfileByUserId(userId)
    }
}
