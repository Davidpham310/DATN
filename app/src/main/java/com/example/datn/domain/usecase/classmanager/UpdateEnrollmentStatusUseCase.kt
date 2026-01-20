package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.repository.IClassRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class UpdateEnrollmentStatusUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(
        classId: String,
        studentId: String,
        status: EnrollmentStatus,
        approvedBy: String = "",
        rejectionReason: String = ""
    ): Flow<Resource<Boolean>> {
        return repository.updateEnrollmentStatus(
            classId, studentId, status, approvedBy, rejectionReason
        )
    }
}
