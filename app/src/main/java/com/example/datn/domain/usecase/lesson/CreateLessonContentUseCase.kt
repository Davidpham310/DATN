package com.example.datn.domain.usecase.lesson

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonContentRepository
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject

// ==================== CREATE ====================
data class CreateLessonContentParams(
    val lessonId: String,
    val title: String,
    val contentType: ContentType,
    val contentText: String? = null,   // Nếu contentType = TEXT, lưu trực tiếp
    val fileStream: InputStream? = null, // Nếu contentType != TEXT, upload file
    val fileSize: Long = 0,
    val onUploadProgress: ((uploaded: Long, total: Long) -> Unit)? = null
)

class CreateLessonContentUseCase @Inject constructor(
    private val repository: ILessonContentRepository
) {
    suspend operator fun invoke(params: CreateLessonContentParams): Flow<Resource<LessonContent?>> {
        val content = LessonContent(
            id = "",
            lessonId = params.lessonId,
            title = params.title,
            contentType = params.contentType,
            content = params.contentText ?: "",
            order = 0,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return repository.addContent(content, params.fileStream, params.fileSize, params.onUploadProgress)
    }
}