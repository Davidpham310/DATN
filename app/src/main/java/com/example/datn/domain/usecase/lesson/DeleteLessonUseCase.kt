package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.data.repository.impl.LessonRepositoryImpl
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class DeleteLessonUseCase @Inject constructor(
    private val repository: LessonRepositoryImpl
) {
    operator fun invoke(lessonId: String): Flow<Resource<Boolean>> {
        return repository.deleteLesson(lessonId)
    }
}