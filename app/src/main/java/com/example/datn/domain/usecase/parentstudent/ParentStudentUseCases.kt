package com.example.datn.domain.usecase.parentstudent

import javax.inject.Inject

data class ParentStudentUseCases @Inject constructor(
    val createStudentAccount: CreateStudentAccountUseCase,
    val linkStudent: LinkStudentUseCase,
    val updateStudentInfo: UpdateStudentInfoUseCase,
    val getLinkedStudents: GetLinkedStudentsUseCase,
    val unlinkStudent: UnlinkStudentUseCase,
    val searchStudent: SearchStudentUseCase,
    val updateRelationship: UpdateRelationshipUseCase
)

