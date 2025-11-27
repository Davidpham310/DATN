package com.example.datn.domain.usecase.parentstudent

import javax.inject.Inject

data class ParentStudentUseCases @Inject constructor(
    val searchStudent: SearchStudentUseCase,
    val updateRelationship: UpdateRelationshipUseCase,
    val unlinkStudent: UnlinkStudentUseCase,
    val getStudentClassesForParent: GetStudentClassesForParentUseCase,
    val getLinkedStudents: GetLinkedStudentsUseCase,
    val linkParentToStudent: LinkParentToStudentUseCase,
    val createStudentAccountForParent: CreateStudentAccountForParentUseCase
)
