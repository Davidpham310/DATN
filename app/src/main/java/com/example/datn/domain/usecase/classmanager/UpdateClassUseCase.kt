package com.example.datn.domain.usecase.classmanager

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.Resource.*
import com.example.datn.domain.models.Class
import com.example.datn.domain.repository.IClassRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject

data class UpdateClassParams(
    val id: String,
    val name: String,
    val classCode: String,
    val teacherId: String,
    val gradeLevel: Int,
    val subject: String?
)

class UpdateClassUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(params: UpdateClassParams): Flow<Resource<Boolean>> = flow {
        Log.d("UpdateClassUseCase", "🔄 BẮT ĐẦU CẬP NHẬT: ${params.id}")

        emit(Resource.Loading())

        try {
            // 1. TÌM LỚP CŨ - Collect toàn bộ flow
            var oldClass: Class? = null
            var fetchError: String? = null

            repository.getClassById(params.id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        oldClass = result.data
                    }
                    is Resource.Error -> {
                        fetchError = result.message
                    }
                    is Resource.Loading -> {
                        // Skip loading
                    }
                }
            }

            Log.d("UpdateClassUseCase", "🔍 TÌM XONG LỚP: ${oldClass?.name}")

            // 2. Kiểm tra lỗi khi fetch
            if (fetchError != null) {
                Log.e("UpdateClassUseCase", "❗ Lỗi tìm lớp cũ: $fetchError")
                emit(Resource.Error(fetchError!!))
                return@flow
            }

            if (oldClass == null) {
                Log.e("UpdateClassUseCase", "❗ Không tìm thấy lớp để cập nhật")
                emit(Resource.Error("Không tìm thấy lớp để cập nhật"))
                return@flow
            }

            // 3. Tạo đối tượng Class mới để cập nhật
            val updatedClass = oldClass!!.copy(
                name = params.name,
                classCode = params.classCode,
                gradeLevel = params.gradeLevel,
                subject = params.subject,
                teacherId = params.teacherId,
                createdAt = oldClass!!.createdAt,
                updatedAt = Instant.now()
            )

            // 4. CẬP NHẬT LỚP - Collect toàn bộ flow
            var updateSuccess = false
            var updateError: String? = null

            repository.updateClass(params.id, updatedClass).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        updateSuccess = result.data
                    }
                    is Resource.Error -> {
                        updateError = result.message
                    }
                    is Resource.Loading -> {
                        // Skip loading
                    }
                }
            }

            // 5. Xử lý kết quả cập nhật
            if (updateError != null) {
                Log.e("UpdateClassUseCase", "❌ Lỗi cập nhật: $updateError")
                emit(Resource.Error(updateError!!))
            } else if (updateSuccess) {
                Log.d("UpdateClassUseCase", "✅ CẬP NHẬT THÀNH CÔNG")
                emit(Resource.Success(true))
            } else {
                Log.e("UpdateClassUseCase", "❌ Cập nhật thất bại")
                emit(Resource.Error("Cập nhật lớp thất bại"))
            }

        } catch (e: Exception) {
            Log.e("UpdateClassUseCase", "💥 Exception: ${e.message}", e)
            emit(Resource.Error(e.message ?: "Lỗi không xác định khi cập nhật lớp học"))
        }
    }
}