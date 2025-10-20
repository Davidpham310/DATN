package com.example.datn.domain.usecase.lesson

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.data.repository.impl.LessonRepositoryImpl
import com.example.datn.domain.models.Lesson
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private val repository: LessonRepositoryImpl
) {
    operator fun invoke(params: UpdateLessonParams): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Lấy lesson cũ
            var oldLesson: Lesson? = null
            var fetchError: String? = null

            repository.getLessonById(params.id).collect { result ->
                when (result) {
                    is Resource.Success -> oldLesson = result.data
                    is Resource.Error -> fetchError = result.message
                    is Resource.Loading -> {} // skip
                }
            }

            if (fetchError != null) {
                emit(Resource.Error(fetchError!!))
                return@flow
            }

            if (oldLesson == null) {
                emit(Resource.Error("Không tìm thấy bài học để cập nhật"))
                return@flow
            }

            // 2. Tạo bản lesson mới, giữ createdAt cũ
            val updatedLesson = oldLesson!!.copy(
                title = params.title,
                description = params.description,
                contentLink = params.contentLink,
                order = params.order,
                teacherId = params.teacherId,
                classId = params.classId,
                updatedAt = Instant.now()
            )

            // 3. Cập nhật lesson
            repository.updateLesson(params.id, updatedLesson).collect { updateResult ->
                when (updateResult) {
                    is Resource.Success -> emit(Resource.Success(updateResult.data))
                    is Resource.Error -> emit(Resource.Error(updateResult.message ?: "Cập nhật bài học thất bại"))
                    is Resource.Loading -> emit(Resource.Loading())
                }
            }

        } catch (e: Exception) {
            Log.e("UpdateLessonUseCase", "💥 Exception: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Lỗi không xác định khi cập nhật bài học"))
        }
    }
}
