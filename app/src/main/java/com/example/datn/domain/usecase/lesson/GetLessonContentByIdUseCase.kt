package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLessonContentByIdUseCase @Inject constructor(
    private val repository: ILessonContentRepository
) {
    operator fun invoke(contentId: String): Flow<Resource<LessonContent>> {
        return repository.getContentById(contentId)
    }
}