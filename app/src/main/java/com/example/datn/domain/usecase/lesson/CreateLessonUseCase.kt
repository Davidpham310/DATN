package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.repository.ILessonRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

// ==================== CREATE ====================

data class CreateLessonParams(
    val classId: String,
    val teacherId: String,
    val title: String,
    val description: String?
)
class CreateLessonUseCase @Inject constructor(
    private val repository: ILessonRepository
) {
    operator fun invoke(params: CreateLessonParams): Flow<Resource<Lesson>> {
        val lesson = Lesson(
            id = "",
            teacherId = params.teacherId,
            classId = params.classId,
            title = params.title,
            description = params.description,
            order = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return repository.createLesson(lesson)
    }
}

