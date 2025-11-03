package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.RelationshipType
import com.example.datn.core.network.service.parent.ParentStudentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class LinkStudentParams(
    val parentId: String,
    val studentId: String,
    val relationship: RelationshipType,
    val isPrimaryGuardian: Boolean
)

class LinkStudentUseCase @Inject constructor(
    private val parentStudentService: ParentStudentService
) : BaseUseCase<LinkStudentParams, Flow<Resource<Unit>>> {
    override fun invoke(params: LinkStudentParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Validate IDs before creating link
            if (params.parentId.isBlank()) {
                emit(Resource.Error("Parent ID không hợp lệ"))
                return@flow
            }
            if (params.studentId.isBlank() || params.studentId == "students") {
                emit(Resource.Error("Student ID không hợp lệ"))
                return@flow
            }
            
            val success = parentStudentService.createParentStudentLink(
                params.parentId,
                params.studentId,
                params.relationship,
                params.isPrimaryGuardian
            )
            if (success) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Không thể tạo liên kết"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Không thể tạo liên kết"))
        }
    }
}

