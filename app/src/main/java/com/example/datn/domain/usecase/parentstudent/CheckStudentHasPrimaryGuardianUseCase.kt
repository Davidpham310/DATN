package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.network.service.parent.ParentStudentService
import com.example.datn.core.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CheckStudentHasPrimaryGuardianUseCase @Inject constructor(
    private val parentStudentService: ParentStudentService
) : BaseUseCase<String, Flow<Resource<Boolean>>> {

    override fun invoke(params: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (params.isBlank()) {
                emit(Resource.Success(false))
                return@flow
            }

            val links = parentStudentService.getParentsByStudentId(params)
            val hasPrimaryGuardian = links.any { it.isPrimaryGuardian }
            emit(Resource.Success(hasPrimaryGuardian))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể kiểm tra người giám hộ chính"))
        }
    }
}
