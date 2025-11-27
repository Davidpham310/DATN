package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.base.BaseUseCase
import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IStudentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class LinkParentToStudentParams(
    val studentId: String,
    val parentId: String,
    val relationship: String
)

class LinkParentToStudentUseCase @Inject constructor(
    private val studentRepository: IStudentRepository
) : BaseUseCase<LinkParentToStudentParams, Flow<Resource<Unit>>> {

    override fun invoke(params: LinkParentToStudentParams): Flow<Resource<Unit>> {
        return studentRepository.linkParentToStudent(
            studentId = params.studentId,
            parentId = params.parentId,
            relationship = params.relationship
        )
    }
}
