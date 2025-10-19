package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class DeleteLessonUseCase @Inject constructor(
    private val repository: com.example.datn.data.repository.impl.LessonRepositoryImpl
) {
    operator fun invoke(lessonId: String): Flow<Resource<Boolean>> {
        return repository.deleteLesson(lessonId)
    }
}