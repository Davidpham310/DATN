package com.example.datn.domain.usecase.classmanager

import javax.inject.Inject

data class ClassUseCases @Inject constructor(
    // Class CRUD
    val addClass: AddClassUseCase,
    val updateClass: UpdateClassUseCase,
    val deleteClass: DeleteClassUseCase,
    val getAllClasses: GetAllClassesUseCase,
    val getClassById: GetClassByIdUseCase,
    val getClassByCode: GetClassByCodeUseCase,
    val getClassesByTeacher: GetClassesByTeacherUseCase,
    val getClassesByStudent: GetClassesByStudentUseCase,

    // Enrollment Management
    val addStudentToClass: AddStudentToClassUseCase,
    val removeStudentFromClass: RemoveStudentFromClassUseCase,
    val approveEnrollment: ApproveEnrollmentUseCase,
    val rejectEnrollment: RejectEnrollmentUseCase,
    val updateEnrollmentStatus: UpdateEnrollmentStatusUseCase,

    // Enrollment Queries
    val getStudentsInClass: GetStudentsInClassUseCase,
    val getApprovedStudentsInClass: GetApprovedStudentsInClassUseCase,
    val getPendingEnrollments: GetPendingEnrollmentsUseCase,
    val getEnrollment: GetEnrollmentUseCase,
    val isStudentInClass: IsStudentInClassUseCase,
    val hasPendingEnrollment: HasPendingEnrollmentUseCase,

    // Batch Operations
    val batchApproveEnrollments: BatchApproveEnrollmentsUseCase,
    val batchRemoveStudentsFromClass: BatchRemoveStudentsFromClassUseCase
)