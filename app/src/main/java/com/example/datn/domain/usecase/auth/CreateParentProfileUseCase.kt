package com.example.datn.domain.usecase.auth

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.core.network.service.parent.ParentProfileService
import com.example.datn.domain.models.Parent
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class CreateParentProfileParams(
    val userId: String
)

class CreateParentProfileUseCase @Inject constructor(
    private val parentProfileService: ParentProfileService
) : BaseUseCase<CreateParentProfileParams, Flow<Resource<Unit>>> {

    override fun invoke(params: CreateParentProfileParams): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val now = Instant.now()
            val parentId = params.userId

            val parent = Parent(
                id = parentId,
                userId = params.userId,
                createdAt = now,
                updatedAt = now
            )

            // Lưu Parent vào collection "parents" với id cố định = parentId
            parentProfileService.add(parentId, parent)

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi tạo hồ sơ phụ huynh"))
        }
    }
}
