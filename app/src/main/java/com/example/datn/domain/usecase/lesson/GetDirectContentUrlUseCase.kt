package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.ILessonContentRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetDirectContentUrlUseCase @Inject constructor(
    private val repository: ILessonContentRepository
) {
    operator fun invoke(path : String): Flow<Resource<String>> {
        return repository.getDirectContentUrl(path)
    }
}