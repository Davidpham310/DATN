package com.example.datn.domain.usecase.parent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.core.network.service.parent.ParentService
import com.example.datn.domain.models.Parent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetParentProfileUseCase @Inject constructor(
    private val parentService: ParentService
) : BaseUseCase<String, Flow<Resource<Parent>>> {

    override fun invoke(parentId: String): Flow<Resource<Parent>> = flow {
        emit(Resource.Loading())
        try {
            val parent = parentService.getById(parentId)
            if (parent == null) {
                emit(Resource.Error("Không tìm thấy hồ sơ phụ huynh"))
            } else {
                emit(Resource.Success(parent))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi lấy hồ sơ phụ huynh"))
        }
    }
}
