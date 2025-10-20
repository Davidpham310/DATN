package com.example.datn.data.repository.impl

import android.util.Log
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.network.service.minio.MinIOService
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonContentRepository
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.LessonContentDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import javax.inject.Inject

private const val TAG = "LessonContentRepoImpl"

class LessonContentRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val lessonContentDao: LessonContentDao
) : ILessonContentRepository {

    override fun getContentByLesson(lessonId: String): Flow<Resource<List<LessonContent>>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Lấy cache Room
            val cachedEntities = lessonContentDao.getContentsByLessonId(lessonId)
            val localContents = cachedEntities.map { it.toDomain() }
            if (localContents.isNotEmpty()) {
                emit(Resource.Success(localContents))
            }

            // 2. Lấy từ FirebaseDataSource
            val result = firebaseDataSource.getLessonContent(lessonId)
            when (result) {
                is Resource.Success -> {
                    val data = result.data ?: emptyList()
                    // 3. Cập nhật cache Room và emit
                    lessonContentDao.insertAll(data.map { it.toEntity() })
                    emit(Resource.Success(data))
                }
                is Resource.Error -> {
                    // Chỉ emit lỗi nếu không có dữ liệu local
                    if (localContents.isEmpty()) {
                        emit(Resource.Error(result.message ?: "Lỗi lấy nội dung"))
                    }
                }
                is Resource.Loading -> { /* Bỏ qua */ }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getContentByLesson", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getContentById(contentId: String): Flow<Resource<LessonContent>> = flow {
        emit(Resource.Loading())
        try {
            val localContent = lessonContentDao.getContentById(contentId)?.toDomain()
            if (localContent != null) {
                emit(Resource.Success(localContent))
            }

            // Fetch from remote
            val result = firebaseDataSource.getLessonContent(contentId)
            when (result) {
                is Resource.Success -> {
                    val data = result.data?.firstOrNull()
                    if (data != null) {
                        lessonContentDao.insert(data.toEntity())
                        emit(Resource.Success(data))
                    } else if (localContent == null) {
                        // Chỉ emit lỗi nếu không có cả local và remote
                        emit(Resource.Error("Nội dung không tồn tại"))
                    }
                }
                is Resource.Error -> {
                    if (localContent == null) {
                        emit(Resource.Error(result.message ?: "Lỗi lấy nội dung theo ID"))
                    }
                }
                is Resource.Loading -> { /* Bỏ qua */ }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getContentById", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun addContent(
        content: LessonContent,
        fileStream: InputStream?,
        fileSize: Long
    ): Flow<Resource<LessonContent>> = flow {
        emit(Resource.Loading())
        try {
            var newContent = content

            // 1. Xử lý upload file nếu cần
            if (content.contentType != ContentType.TEXT && fileStream != null) {
                // Tạo một ID tạm thời/được tạo trước để xây dựng objectName
                val objectName = "lesson/${content.lessonId}/${content.id}-${content.title}"
                MinIOService.uploadFile(objectName, fileStream, fileSize, content.contentType.name)
                // Cập nhật content (link file) và id tạm thời
                newContent = content.copy(content = objectName)
            }

            // 2. Thêm vào Firebase
            val addedResource = firebaseDataSource.addLessonContent(newContent)
            when (addedResource) {
                is Resource.Success -> {
                    addedResource.data?.let { added ->
                        lessonContentDao.insert(added.toEntity())
                        emit(Resource.Success(added))
                    } ?: emit(Resource.Error("Failed to add content. Data is null."))
                }
                is Resource.Error -> emit(Resource.Error(addedResource.message))
                is Resource.Loading -> { /* Bỏ qua */ }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error addContent", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun updateContent(
        contentId: String,
        content: LessonContent,
        newFileStream: InputStream?,
        newFileSize: Long
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Xử lý update file nếu cần
            if (content.contentType != ContentType.TEXT && newFileStream != null) {
                if (content.content.isNotEmpty()) {
                    MinIOService.updateFile(content.content, newFileStream, newFileSize, content.contentType.name)
                } else {
                    // Nếu chưa có link file cũ, upload file mới
                    val objectName = "lesson/${content.lessonId}/${content.id}-${content.title}"
                    MinIOService.uploadFile(objectName, newFileStream, newFileSize, content.contentType.name)
                }
            }

            // 2. Cập nhật Firebase
            val result = firebaseDataSource.updateLessonContent(contentId ,content)
            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        lessonContentDao.update(content.toEntity())
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to update content"))
                    }
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> { /* Bỏ qua */ }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updateContent", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun deleteContent(contentId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Lấy nội dung để xóa file MinIO
            val contentResource = firebaseDataSource.getLessonContent(contentId)
            val content = (contentResource as? Resource.Success)?.data?.firstOrNull()

            content?.let {
                if (it.contentType != ContentType.TEXT && it.content.isNotEmpty()) {
                    MinIOService.deleteFile(it.content)
                }
            }

            // 2. Xóa Firebase
            val result = firebaseDataSource.deleteLessonContent(contentId)
            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        lessonContentDao.deleteById(contentId)
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to delete content"))
                    }
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> { /* Bỏ qua */ }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleteContent", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }
}