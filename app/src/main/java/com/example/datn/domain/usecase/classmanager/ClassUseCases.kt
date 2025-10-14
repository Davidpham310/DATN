package com.example.datn.domain.usecase.classmanager

import jakarta.inject.Inject

data class ClassUseCases @Inject constructor(
    val addClass: AddClassUseCase,
    val deleteClass: DeleteClassUseCase,
    val getAllClasses: GetAllClassesUseCase,
    val getClassesByTeacher: GetClassesByTeacherUseCase
)