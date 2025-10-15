package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.repository.IClassRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetEnrollmentUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(classId: String, studentId: String): Flow<Resource<ClassStudent?>> {
        return repository.getEnrollment(classId, studentId)
    }
}