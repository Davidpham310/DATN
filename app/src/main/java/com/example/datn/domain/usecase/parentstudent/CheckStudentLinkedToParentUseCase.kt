package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.network.service.parent.ParentStudentService
import com.example.datn.core.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class CheckStudentLinkedToParentParams(
    val parentId: String,
    val studentId: String
)

class CheckStudentLinkedToParentUseCase @Inject constructor(
    private val parentStudentService: ParentStudentService
) : BaseUseCase<CheckStudentLinkedToParentParams, Flow<Resource<Boolean>>> {

    override fun invoke(params: CheckStudentLinkedToParentParams): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (params.parentId.isBlank() || params.studentId.isBlank()) {
                emit(Resource.Success(false))
                return@flow
            }

            val linked = parentStudentService.isLinked(params.parentId, params.studentId)
            emit(Resource.Success(linked))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể kiểm tra liên kết học sinh"))
        }
    }
}
