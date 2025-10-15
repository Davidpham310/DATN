package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IClassRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class BatchApproveEnrollmentsUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(
        classId: String,
        studentIds: List<String>,
        approvedBy: String
    ): Flow<Resource<List<Boolean>>> {
        return repository.batchApproveEnrollments(classId, studentIds, approvedBy)
    }
}