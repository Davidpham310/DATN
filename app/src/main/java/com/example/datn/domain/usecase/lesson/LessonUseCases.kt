package com.example.datn.domain.usecase.lesson

import javax.inject.Inject
import com.example.datn.domain.usecase.progress.UpdateLessonProgressUseCase

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
    val getLessonContentsByLesson: GetLessonContentsByLessonUseCase,
    val getLessonContentUrl: GetLessonContentUrlUseCase,
    val getDirectLessonContentUrl: GetDirectContentUrlUseCase,

    // LessonProgress
    val updateLessonProgress: UpdateLessonProgressUseCase,
    val getLessonList: GetLessonListUseCase,
    val getLessonContents: GetLessonContentsUseCase
)