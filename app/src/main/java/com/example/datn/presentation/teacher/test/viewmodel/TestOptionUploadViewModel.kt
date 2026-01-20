package com.example.datn.presentation.teacher.test.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseEvent
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.datn.domain.usecase.minio.MinIOUseCase
import java.io.InputStream

data class TestOptionUploadState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isUploading: Boolean = false,
    val uploadProgressPercent: Int = 0,
    val selectedFileName: String? = null,
    val uploadedUrl: String? = null
) : BaseState

sealed interface TestOptionUploadEvent : BaseEvent

@HiltViewModel
class TestOptionUploadViewModel @Inject constructor(
    private val minIOUseCase: MinIOUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<TestOptionUploadState, TestOptionUploadEvent>(TestOptionUploadState(), notificationManager) {

    fun reset() {
        setState { TestOptionUploadState() }
    }

    fun uploadImage(questionId: String, fileName: String, inputStream: InputStream, size: Long) {
        viewModelScope.launch {
            try {
                setState { copy(isUploading = true, error = null, selectedFileName = fileName, uploadProgressPercent = 0, uploadedUrl = null) }

                val (ext, mime) = when {
                    fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> ".jpg" to "image/jpeg"
                    fileName.endsWith(".png", true) -> ".png" to "image/png"
                    fileName.endsWith(".gif", true) -> ".gif" to "image/gif"
                    fileName.endsWith(".webp", true) -> ".webp" to "image/webp"
                    else -> ".jpg" to "image/jpeg"
                }

                val objectName = "test_options/$questionId/option_${System.currentTimeMillis()}$ext"

                minIOUseCase.uploadFile(
                    objectName = objectName,
                    inputStream = inputStream,
                    size = size,
                    contentType = mime
                ) { uploaded, total ->
                    val safeTotal = if (total > 0) total else 1L
                    val percent = ((uploaded * 100) / safeTotal).toInt().coerceIn(0, 100)
                    setState { copy(uploadProgressPercent = percent, isUploading = true) }
                }

                // Store the relative MinIO object key for persistence in Firebase
                setState { copy(isUploading = false, uploadedUrl = objectName, uploadProgressPercent = 100) }
                showNotification("Tải ảnh lên thành công", NotificationType.SUCCESS)
            } catch (e: Exception) {
                setState { copy(isUploading = false, error = e.message) }
                showNotification(e.message ?: "Lỗi tải ảnh lên", NotificationType.ERROR)
            } finally {
                try { inputStream.close() } catch (_: Exception) {}
            }
        }
    }

    override fun onEvent(event: TestOptionUploadEvent) {
        // No UI events defined for upload flow yet
    }
}
