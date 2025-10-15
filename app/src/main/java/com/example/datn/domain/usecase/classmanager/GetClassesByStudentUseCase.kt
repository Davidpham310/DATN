package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IClassRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import com.example.datn.domain.models.Class

class GetClassesByStudentUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(studentId: String): Flow<Resource<List<Class>>> {
        return repository.getClassesByStudent(studentId)
    }
}