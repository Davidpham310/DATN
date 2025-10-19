package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class UpdateLessonContentUseCase @Inject constructor(
    private val repository: ILessonRepository
) {
    operator fun invoke(content: LessonContent): Flow<Resource<Unit>> {
        return repository.updateLessonContent(content)
    }
}