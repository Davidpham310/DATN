package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonContentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLessonContentsByLessonUseCase @Inject constructor(
    private val repository: ILessonContentRepository
) {
    operator fun invoke(lessonId: String): Flow<Resource<List<LessonContent>>> {
        return repository.getContentByLesson(lessonId)
    }
}
