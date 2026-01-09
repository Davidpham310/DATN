package com.example.datn.domain.usecase.lesson

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.io.InputStream
import javax.inject.Inject

data class UpdateLessonContentParams(
    val contentId: String,
    val lessonId: String,
    val title: String,
    val contentType: ContentType,
    val contentText: String? = null,    // Nếu contentType = TEXT, lưu trực tiếp
    val order: Int,
    val newFileStream: InputStream? = null,  // Nếu cập nhật file
    val newFileSize: Long = 0,
    val onUploadProgress: ((uploaded: Long, total: Long) -> Unit)? = null
)

class UpdateLessonContentUseCase @Inject constructor(
    private val repository: ILessonContentRepository
) {
    operator fun invoke(params: UpdateLessonContentParams): Flow<Resource<Boolean>> = flow {
        // 1. Emit Loading
        emit(Resource.Loading())

        try {
            // --- BƯỚC 1: Lấy nội dung cũ (Không cần thay đổi) ---
            var oldContent: LessonContent? = null
            var fetchError: String? = null

            // Lấy nội dung cũ (cần collect vì nó trả về Flow)
            repository.getContentById(params.contentId).collect { result ->
                when (result) {
                    is Resource.Success -> oldContent = result.data
                    is Resource.Error -> fetchError = result.message
                    is Resource.Loading -> {} // Bỏ qua trạng thái Loading
                }
            }

            if (fetchError != null) {
                emit(Resource.Error(fetchError!!))
                return@flow
            }

            if (oldContent == null) {
                emit(Resource.Error("Không tìm thấy nội dung để cập nhật"))
                return@flow
            }

            // --- BƯỚC 2: Tạo bản cập nhật ---
            // Nếu là TEXT, cập nhật `content` bằng `contentText` mới.
            // Nếu không phải TEXT, giữ nguyên `oldContent.content` (link file) trừ khi có file mới.
            val updatedContent = oldContent!!.copy(
                title = params.title,
                contentType = params.contentType,
                content = if (params.contentType == ContentType.TEXT) params.contentText ?: "" else oldContent.content,
                order = params.order,
                updatedAt = Instant.now()
            )

            // --- BƯỚC 3: Cập nhật trong repository (Cần sửa) ---
            // Lắng nghe (collect) Flow được trả về từ repository.updateContent
            repository.updateContent(
                contentId = params.contentId,
                content = updatedContent,
                newFileStream = params.newFileStream,
                newFileSize = params.newFileSize,
                onUploadProgress = params.onUploadProgress
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        // Trực tiếp emit kết quả thành công/thất bại từ repository
                        if (result.data) {
                            emit(Resource.Success(true))
                        } else {
                            emit(Resource.Error("Cập nhật nội dung thất bại từ repository"))
                        }
                    }
                    is Resource.Error -> {
                        emit(Resource.Error(result.message ?: "Cập nhật nội dung thất bại"))
                    }
                    is Resource.Loading -> {
                        // Có thể emit Loading lần nữa, nhưng thường chỉ cần emit ở đầu
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("UpdateLessonContentUC", "💥 Exception: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Lỗi không xác định khi cập nhật nội dung"))
        }
    }
}