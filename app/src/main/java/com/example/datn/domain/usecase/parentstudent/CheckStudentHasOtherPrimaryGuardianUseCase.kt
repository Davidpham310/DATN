package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.network.service.parent.ParentStudentService
import com.example.datn.core.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class CheckStudentHasOtherPrimaryGuardianParams(
    val studentId: String,
    val parentId: String
)

class CheckStudentHasOtherPrimaryGuardianUseCase @Inject constructor(
    private val parentStudentService: ParentStudentService
) : BaseUseCase<CheckStudentHasOtherPrimaryGuardianParams, Flow<Resource<Boolean>>> {

    override fun invoke(params: CheckStudentHasOtherPrimaryGuardianParams): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (params.studentId.isBlank() || params.parentId.isBlank()) {
                emit(Resource.Success(false))
                return@flow
            }

            val links = parentStudentService.getParentsByStudentId(params.studentId)
            val hasOtherPrimaryGuardian = links.any { it.isPrimaryGuardian && it.parentId != params.parentId }
            emit(Resource.Success(hasOtherPrimaryGuardian))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể kiểm tra người giám hộ chính"))
        }
    }
}
