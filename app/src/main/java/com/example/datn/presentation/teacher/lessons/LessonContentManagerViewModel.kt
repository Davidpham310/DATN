package com.example.datn.presentation.teacher.lessons

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ContentType
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.lesson.CreateLessonContentParams
import com.example.datn.domain.usecase.lesson.LessonUseCases
import com.example.datn.domain.usecase.lesson.UpdateLessonContentParams
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.lesson.LessonContentManagerEvent
import com.example.datn.presentation.common.lesson.LessonContentManagerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class LessonContentManagerViewModel @Inject constructor(
    private val lessonUseCases: LessonUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<LessonContentManagerState, LessonContentManagerEvent>(
    LessonContentManagerState(), notificationManager
) {

    private var currentTeacherId: String = ""

    init {
        viewModelScope.launch {
            authUseCases.getCurrentIdUser()
                .distinctUntilChanged()
                .collect { id ->
                    currentTeacherId = id
                    Log.d("LessonContentVM", "Loaded teacherId: $currentTeacherId")
                }
        }
    }

    // =====================================================
    // FILE SELECTION
    // =====================================================
    fun onFileSelected(fileName: String, stream: InputStream, size: Long) {
        state.value.selectedFileStream?.close()
        setState {
            copy(
                selectedFileName = fileName,
                selectedFileStream = stream,
                selectedFileSize = size
            )
        }
        showNotification("Đã chọn tệp: $fileName (${size / 1024} KB)", NotificationType.INFO)
    }

    fun resetFileSelection() {
        state.value.selectedFileStream?.close()
        setState {
            copy(
                selectedFileName = null,
                selectedFileStream = null,
                selectedFileSize = 0L
            )
        }
    }

    // =====================================================
    // EVENT HANDLING
    // =====================================================
    override fun onEvent(event: LessonContentManagerEvent) {
        when (event) {
            is LessonContentManagerEvent.LoadContentsForLesson -> loadContents(event.lessonId)
            is LessonContentManagerEvent.RefreshContents -> refreshContents()
            is LessonContentManagerEvent.SelectContent -> setState { copy(selectedContent = event.content) }

            is LessonContentManagerEvent.ShowAddContentDialog -> {
                resetFileSelection()
                setState { copy(showAddEditDialog = true, editingContent = null) }
            }
            is LessonContentManagerEvent.EditContent -> {
                resetFileSelection()
                setState { copy(showAddEditDialog = true, editingContent = event.content) }
            }

            is LessonContentManagerEvent.DeleteContent -> showConfirmDeleteContent(event.content)
            is LessonContentManagerEvent.DismissDialog -> {
                resetFileSelection()
                setState { copy(showAddEditDialog = false, editingContent = null) }
            }

            is LessonContentManagerEvent.ConfirmAddContent -> addContent(event)
            is LessonContentManagerEvent.ConfirmEditContent -> updateContent(event)
        }
    }

    // =====================================================
    // CRUD & CONTENT URL
    // =====================================================
    private fun loadContents(lessonId: String) {
        setState { copy(currentLessonId = lessonId) }
        viewModelScope.launch {
            lessonUseCases.getLessonContentsByLesson(lessonId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        val contents = result.data ?: emptyList()
                        setState {
                            copy(
                                lessonContents = contents,
                                isLoading = false,
                                error = null
                            )
                        }
                        // Tự động load URL media
                        contents.forEach { content ->
                            if (content.contentType != ContentType.TEXT && content.content.isNotEmpty()) {
                                loadContentUrl(content)
                            }
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải nội dung thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun refreshContents() {
        val lessonId = state.value.currentLessonId
        if (lessonId.isNotEmpty()) loadContents(lessonId)
    }

    private fun addContent(event: LessonContentManagerEvent.ConfirmAddContent) {
        if (currentTeacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }

        val type = try {
            ContentType.valueOf(event.contentType.uppercase())
        } catch (e: Exception) {
            showNotification("Loại nội dung không hợp lệ", NotificationType.ERROR)
            return
        }

        if (event.title.isBlank()) {
            showNotification("Tiêu đề không được để trống", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            lessonUseCases.createLessonContent(
                CreateLessonContentParams(
                    lessonId = event.lessonId,
                    title = event.title,
                    contentType = type,
                    contentText = if (type == ContentType.TEXT) event.contentLink else null,
                    fileStream = event.fileStream,
                    fileSize = event.fileSize
                )
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        resetFileSelection()
                        setState { copy(isLoading = false, showAddEditDialog = false) }
                        showNotification("Thêm nội dung thành công!", NotificationType.SUCCESS)
                        refreshContents()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Thêm nội dung thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun updateContent(event: LessonContentManagerEvent.ConfirmEditContent) {
        if (currentTeacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }

        val type = try {
            ContentType.valueOf(event.contentType.uppercase())
        } catch (e: Exception) {
            showNotification("Loại nội dung không hợp lệ", NotificationType.ERROR)
            return
        }

        if (event.title.isBlank()) {
            showNotification("Tiêu đề không được để trống", NotificationType.ERROR)
            return
        }

        val order = state.value.lessonContents.find { it.id == event.id }?.order ?: 0

        viewModelScope.launch {
            lessonUseCases.updateLessonContent(
                UpdateLessonContentParams(
                    contentId = event.id,
                    lessonId = event.lessonId,
                    title = event.title,
                    contentType = type,
                    contentText = if (type == ContentType.TEXT) event.contentLink else null,
                    order = order,
                    newFileStream = event.fileStream,
                    newFileSize = event.fileSize
                )
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        resetFileSelection()
                        setState { copy(isLoading = false, showAddEditDialog = false, editingContent = null) }
                        showNotification("Cập nhật nội dung thành công!", NotificationType.SUCCESS)
                        refreshContents()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Cập nhật nội dung thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun showConfirmDeleteContent(content: LessonContent) {
        setState {
            copy(
                confirmDeleteState = ConfirmationDialogState(
                    isShowing = true,
                    title = "Xác nhận xóa nội dung",
                    message = "Bạn có chắc chắn muốn xóa nội dung \"${content.title}\"?\nHành động này sẽ không thể hoàn tác.",
                    data = content
                )
            )
        }
    }

    fun dismissConfirmDeleteDialog() {
        setState { copy(confirmDeleteState = ConfirmationDialogState.empty()) }
    }

    fun confirmDeleteContent(content: LessonContent) {
        dismissConfirmDeleteDialog()
        viewModelScope.launch {
            lessonUseCases.deleteLessonContent(content.id).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        showNotification("Xóa nội dung thành công!", NotificationType.SUCCESS)
                        refreshContents()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Xóa nội dung thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    // =====================================================
    // LOAD URL MEDIA
    // =====================================================
    fun loadContentUrl(content: LessonContent, expirySeconds: Int = 3600) {
        viewModelScope.launch {
            lessonUseCases.getLessonContentUrl(content, expirySeconds).collect { resource ->
                when (resource) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        val url = resource.data ?: ""
                        Log.d("LessonContentVM", "Loaded URL for content '${content.title}': $url")
                        setState {
                            copy(
                                isLoading = false,
                                contentUrls = state.value.contentUrls + (content.id to url)
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification("Lấy URL thất bại: ${resource.message}", NotificationType.ERROR)
                    }
                }
            }
        }
    }
}
