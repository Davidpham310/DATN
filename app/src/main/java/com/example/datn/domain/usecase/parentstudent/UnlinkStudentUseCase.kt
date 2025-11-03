package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IParentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class UnlinkStudentParams(
    val parentId: String,
    val studentId: String
)

class UnlinkStudentUseCase @Inject constructor(
    private val parentRepository: IParentRepository
) : BaseUseCase<UnlinkStudentParams, Flow<Resource<Unit>>> {
    override fun invoke(params: UnlinkStudentParams): Flow<Resource<Unit>> {
        return parentRepository.unlinkStudent(params.parentId, params.studentId)
    }
}
