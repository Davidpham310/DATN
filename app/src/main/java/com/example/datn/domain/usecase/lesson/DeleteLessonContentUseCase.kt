package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.ILessonContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteLessonContentUseCase @Inject constructor(
    private val repository: ILessonContentRepository
) {
    suspend operator fun invoke(contentId: String): Flow<Resource<Boolean>> {
        return repository.deleteContent(contentId)
    }
}