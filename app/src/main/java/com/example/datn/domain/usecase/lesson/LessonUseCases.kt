package com.example.datn.domain.usecase.lesson

import jakarta.inject.Inject

data class LessonUseCases @Inject constructor(
    // Lesson
    val createLesson: CreateLessonUseCase,
    val getLessonsByClass: GetLessonsByClassUseCase,
    val updateLesson: UpdateLessonUseCase,
    val deleteLesson: DeleteLessonUseCase,
    val getLessonById: GetLessonByIdUseCase,

    // LessonContent
    val createLessonContent: CreateLessonContentUseCase,
    val updateLessonContent: UpdateLessonContentUseCase,
    val deleteLessonContent: DeleteLessonContentUseCase,
    val getLessonContentById: GetLessonContentByIdUseCase,
    val getLessonContentsByLesson: GetLessonContentsByLessonUseCase
)