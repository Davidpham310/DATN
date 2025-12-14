package com.example.datn.domain.usecase.parent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.core.network.service.parent.ParentService
import com.example.datn.domain.models.Parent
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetParentProfileUseCase @Inject constructor(
    private val parentService: ParentService
) : BaseUseCase<String, Flow<Resource<Parent>>> {

    override fun invoke(userId: String): Flow<Resource<Parent>> = flow {
        emit(Resource.Loading())
        try {
            val parent = parentService.getParentByUserId(userId)
                ?: parentService.getById(userId)
            if (parent == null) {
                val now = Instant.now()
                val createdParent = Parent(
                    id = userId,
                    userId = userId,
                    createdAt = now,
                    updatedAt = now
                )
                parentService.add(userId, createdParent)
                emit(Resource.Success(createdParent))
            } else {
                emit(Resource.Success(parent))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy hồ sơ phụ huynh"))
        }
    }
}
