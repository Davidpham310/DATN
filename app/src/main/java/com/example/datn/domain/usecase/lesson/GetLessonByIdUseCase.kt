package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetLessonByIdUseCase @Inject constructor(
    private val repository: com.example.datn.data.repository.impl.LessonRepositoryImpl
) {
    operator fun invoke(lessonId: String): Flow<Resource<Lesson?>> {
        return repository.getLessonById(lessonId)
    }
}
