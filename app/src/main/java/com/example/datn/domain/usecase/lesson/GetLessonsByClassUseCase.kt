package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.repository.ILessonRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetLessonsByClassUseCase @Inject constructor(
    private val repository: ILessonRepository
) {
    operator fun invoke(classId: String): Flow<Resource<List<Lesson>>> {
        return repository.getLessonsByClass(classId)
    }
}