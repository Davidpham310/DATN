package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class UpdateLessonParams(
    val id: String,
    val classId: String,
    val teacherId: String,
    val title: String,
    val description: String?,
    val contentLink: String?,
    val order: Int
)

class UpdateLessonUseCase @Inject constructor(
    private val repository: com.example.datn.data.repository.impl.LessonRepositoryImpl
) {
    operator fun invoke(params: UpdateLessonParams): Flow<Resource<Boolean>> {
        val lesson = Lesson(
            id = params.id,
            teacherId = params.teacherId,
            classId = params.classId,
            title = params.title,
            description = params.description,
            contentLink = params.contentLink,
            order = params.order,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return repository.updateLesson(params.id, lesson)
    }
}