package com.example.datn.data.repository.impl

import android.util.Log
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonContentRepository
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.LessonContentDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.domain.usecase.minio.MinIOUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import javax.inject.Inject

private const val TAG = "LessonContentRepoImpl"

class LessonContentRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val lessonContentDao: LessonContentDao,
    private val minIOUseCase: MinIOUseCase
) : ILessonContentRepository {

    override fun getContentByLesson(lessonId: String): Flow<Resource<List<LessonContent>>> = flow {
        emit(Resource.Loading())
        try {
            val cached = lessonContentDao.getContentsByLessonId(lessonId).map { it.toDomain() }
            if (cached.isNotEmpty()) emit(Resource.Success(cached))

            val result = firebaseDataSource.getLessonContent(lessonId)
            when (result) {
                is Resource.Success -> {
                    val data = result.data ?: emptyList()
                    lessonContentDao.insertAll(data.map { it.toEntity() })
                    emit(Resource.Success(data))
                }
                is Resource.Error -> if (cached.isEmpty()) emit(Resource.Error(result.message))
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getContentByLesson", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getContentById(contentId: String): Flow<Resource<LessonContent>> = flow {
        emit(Resource.Loading())
        try {
            val local = lessonContentDao.getContentById(contentId)?.toDomain()
            if (local != null) emit(Resource.Success(local))

            val result = firebaseDataSource.getLessonContent(contentId)
            when (result) {
                is Resource.Success -> {
                    val data = result.data?.firstOrNull()
                    if (data != null) {
                        lessonContentDao.insert(data.toEntity())
                        emit(Resource.Success(data))
                    } else if (local == null) {
                        emit(Resource.Error("Nội dung không tồn tại"))
                    }
                }
                is Resource.Error -> if (local == null) emit(Resource.Error(result.message))
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getContentById", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    // 🟢 ADD CONTENT + MinIO
    override fun addContent(
        content: LessonContent,
        fileStream: InputStream?,
        fileSize: Long
    ): Flow<Resource<LessonContent>> = flow {
        emit(Resource.Loading())
        try {
            var contentToUpload = content

            if (fileStream != null) {
                val extension = when (content.contentType.name.lowercase()) {
                    "image" -> ".jpg"
                    "video" -> ".mp4"
                    "audio" -> ".mp3"
                    "pdf" -> ".pdf"
                    else -> ""
                }

                val objectName = "lessons/${content.lessonId}/content_${System.currentTimeMillis()}$extension"

                val uploadResult = minIOUseCase.uploadFile(
                    objectName,
                    fileStream,
                    fileSize,
                    content.contentType.name.lowercase()
                )
                Log.i(TAG, "✅ Uploaded file to MinIO: $objectName")
                contentToUpload = contentToUpload.copy(content = objectName)
            }

            val added = firebaseDataSource.addLessonContent(contentToUpload, null, 0)
            when (added) {
                is Resource.Success -> {
                    added.data?.let {
                        lessonContentDao.insert(it.toEntity())
                        emit(Resource.Success(it))
                    } ?: emit(Resource.Error("Không thêm được nội dung"))
                }
                is Resource.Error -> emit(Resource.Error(added.message))
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error addContent", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        } finally {
            fileStream?.close()
        }
    }

    // 🟡 UPDATE CONTENT + MinIO
    override fun updateContent(
        contentId: String,
        content: LessonContent,
        newFileStream: InputStream?,
        newFileSize: Long
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            // 🔹 Lấy bản ghi cũ từ local DB
            val oldContent = lessonContentDao.getContentById(contentId)?.toDomain()

            // 🔹 Nếu không tồn tại thì báo lỗi
            if (oldContent == null) {
                emit(Resource.Error("Không tìm thấy nội dung cũ để cập nhật"))
                return@flow
            }

            var updatedContent = content.copy(
                createdAt = oldContent.createdAt
            )

            // 🔹 Nếu có file mới thì upload lên MinIO
            if (newFileStream != null) {
                val extension = when (content.contentType.name.lowercase()) {
                    "image" -> ".jpg"
                    "video" -> ".mp4"
                    "audio" -> ".mp3"
                    "pdf" -> ".pdf"
                    else -> ""
                }

                val newObject =
                    "lessons/${content.lessonId}/content_${System.currentTimeMillis()}$extension"

                minIOUseCase.uploadFile(
                    newObject,
                    newFileStream,
                    newFileSize,
                    content.contentType.name.lowercase()
                )
                Log.i(TAG, "✅ Uploaded new file to MinIO: $newObject")

                // 🔹 Xóa file cũ nếu có
                if (oldContent.content.startsWith("lessons/")) {
                    try {
                        minIOUseCase.deleteFile(oldContent.content)
                        Log.i(TAG, "🗑️ Deleted old MinIO file: ${oldContent.content}")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to delete old MinIO file", e)
                    }
                }

                // 🔹 Cập nhật lại đường dẫn file
                updatedContent = updatedContent.copy(content = newObject)
            }

            // 🔹 Cập nhật Firestore
            val result = firebaseDataSource.updateLessonContent(
                contentId,
                updatedContent,
                null,
                0
            )

            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        // 🔹 Cập nhật local DB
                        lessonContentDao.update(updatedContent.toEntity())
                        emit(Resource.Success(true))
                    } else emit(Resource.Error("Cập nhật thất bại"))
                }

                is Resource.Error -> emit(Resource.Error(result.message))
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updateContent", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        } finally {
            newFileStream?.close()
        }
    }


    // 🔴 DELETE CONTENT + MinIO
    override fun deleteContent(contentId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val old = lessonContentDao.getContentById(contentId)?.toDomain()

            val result = firebaseDataSource.deleteLessonContent(contentId)
            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        lessonContentDao.deleteById(contentId)
                        if (old?.content?.startsWith("lessons/") == true) {
                            try {
                                minIOUseCase.deleteFile(old.content)
                                Log.i(TAG, "🗑️ Deleted MinIO file: ${old.content}")
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Failed to delete MinIO file", e)
                            }
                        }
                        emit(Resource.Success(true))
                    } else emit(Resource.Error("Xóa thất bại"))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                else -> {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleteContent", e)
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getContentUrl(content: LessonContent, expirySeconds: Int): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            val url = minIOUseCase.getFileUrl(content.content, expirySeconds)
            emit(Resource.Success(url))
        }.catch { e ->
            emit(Resource.Error(e.localizedMessage ?: "Lấy URL thất bại"))
        }
    }

    override fun getDirectContentUrl(path : String): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            val url = minIOUseCase.getDirectFileUrl(path)
            emit(Resource.Success(url))
        }.catch { e ->
            emit(Resource.Error(e.localizedMessage ?: "Lấy URL trực tiếp thất bại"))
        }
    }
}
