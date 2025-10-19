package com.example.datn.domain.usecase.lesson

import jakarta.inject.Inject

data class LessonUseCases @Inject constructor(
    val createLesson: CreateLessonUseCase,
    val getLessonsByClass: GetLessonsByClassUseCase,
    val getLessonContent: GetLessonContentUseCase,
    val updateLesson: UpdateLessonUseCase,
    val updateLessonContent: UpdateLessonContentUseCase,
    val deleteLesson: DeleteLessonUseCase,
    val getLessonById: GetLessonByIdUseCase
)