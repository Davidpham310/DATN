package com.example.datn.domain.usecase.parent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.core.network.service.parent.ParentService
import com.example.datn.domain.models.Parent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class UpdateParentProfileParams(
    val parentId: String
)

class UpdateParentProfileUseCase @Inject constructor(
    private val parentService: ParentService
) : BaseUseCase<UpdateParentProfileParams, Flow<Resource<Unit>>> {

    override fun invoke(params: UpdateParentProfileParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val parent = parentService.getById(params.parentId)
            if (parent == null) {
                emit(Resource.Error("Không tìm thấy hồ sơ phụ huynh"))
                return@flow
            }

            val updatedParent = parent.copy(
                updatedAt = java.time.Instant.now()
            )

            parentService.update(params.parentId, updatedParent)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi cập nhật hồ sơ phụ huynh"))
        }
    }
}
