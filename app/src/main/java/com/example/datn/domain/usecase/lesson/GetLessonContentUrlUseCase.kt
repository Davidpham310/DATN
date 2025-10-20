package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLessonContentUrlUseCase @Inject constructor(
    private val repository: ILessonContentRepository
) {
    operator fun invoke(
        content: LessonContent,
        expirySeconds: Int = 3600
    ): Flow<Resource<String>> {
        return repository.getContentUrl(content, expirySeconds)
    }
}