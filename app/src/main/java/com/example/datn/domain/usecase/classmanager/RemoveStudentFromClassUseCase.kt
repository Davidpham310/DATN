package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IClassRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class RemoveStudentFromClassUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(classId: String, studentId: String): Flow<Resource<Boolean>> {
        return repository.removeStudentFromClass(classId, studentId)
    }
}