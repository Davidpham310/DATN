package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IClassRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RejectEnrollmentUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(
        classId: String,
        studentId: String,
        rejectionReason: String,
        rejectedBy: String
    ): Flow<Resource<Boolean>> {
        return repository.rejectEnrollment(classId, studentId, rejectionReason, rejectedBy)
    }
}