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
import java.io.InputStream // Cần thiết để xử lý InputStream
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

    // ====================================================================
    // 🛠️ QUẢN LÝ TỆP TIN (FILE SELECTION)
    // ====================================================================

    /**
     * Cập nhật trạng thái tệp tin đã chọn vào State.
     */
    fun onFileSelected(fileName: String, stream: InputStream, size: Long) {
        // Đóng InputStream cũ trước khi gán cái mới
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

    /**
     * Đặt lại trạng thái tệp tin đã chọn.
     * Cần gọi khi đóng dialog, trước khi mở dialog mới, hoặc sau khi CRUD thành công.
     */
    fun resetFileSelection() {
        // Đóng InputStream cũ nếu tồn tại
        state.value.selectedFileStream?.close()
        setState {
            copy(
                selectedFileName = null,
                selectedFileStream = null,
                selectedFileSize = 0L
            )
        }
    }

    // ====================================================================
    // ⬇️ XỬ LÝ SỰ KIỆN (EVENT HANDLING)
    // ====================================================================

    override fun onEvent(event: LessonContentManagerEvent) {
        when (event) {
            is LessonContentManagerEvent.LoadContentsForLesson -> loadContents(event.lessonId)
            is LessonContentManagerEvent.RefreshContents -> refreshContents()
            is LessonContentManagerEvent.SelectContent -> setState { copy(selectedContent = event.content) }

            is LessonContentManagerEvent.ShowAddContentDialog -> {
                resetFileSelection() // Reset trạng thái file khi mở dialog ADD
                setState { copy(showAddEditDialog = true, editingContent = null) }
            }
            is LessonContentManagerEvent.EditContent -> {
                resetFileSelection() // Reset trạng thái file khi mở dialog EDIT
                setState { copy(showAddEditDialog = true, editingContent = event.content) }
            }

            is LessonContentManagerEvent.DeleteContent -> showConfirmDeleteContent(event.content)

            is LessonContentManagerEvent.DismissDialog -> {
                resetFileSelection() // Reset trạng thái file khi đóng dialog
                setState { copy(showAddEditDialog = false, editingContent = null) }
            }

            is LessonContentManagerEvent.ConfirmAddContent -> addContent(event)
            is LessonContentManagerEvent.ConfirmEditContent -> updateContent(event)
        }
    }

    // ====================================================================
    // ⬆️ HÀM CRUD
    // ====================================================================

    private fun loadContents(lessonId: String) {
        setState { copy(currentLessonId = lessonId) }
        viewModelScope.launch {
            lessonUseCases.getLessonContentsByLesson(lessonId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        setState {
                            copy(
                                lessonContents = result.data ?: emptyList(),
                                isLoading = false,
                                error = null
                            )
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
        if (lessonId.isNotEmpty()) {
            loadContents(lessonId)
        }
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

        // Lấy thông tin file từ Event (đã được Composable truyền từ State của ViewModel)
        val fileStream = event.fileStream
        val fileSize = event.fileSize

        viewModelScope.launch {
            lessonUseCases.createLessonContent(
                CreateLessonContentParams(
                    lessonId = event.lessonId,
                    title = event.title,
                    contentType = type,
                    contentText = if (type == ContentType.TEXT) event.contentLink else null,
                    fileStream = fileStream,
                    fileSize = fileSize
                )
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        resetFileSelection() // Reset stream sau khi thành công
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

        // Lấy thông tin file mới từ Event
        val newFileStream = event.fileStream
        val newFileSize = event.fileSize

        viewModelScope.launch {
            lessonUseCases.updateLessonContent(
                UpdateLessonContentParams(
                    contentId = event.id,
                    lessonId = event.lessonId,
                    title = event.title,
                    contentType = type,
                    contentText = if (type == ContentType.TEXT) event.contentLink else null,
                    order = order,
                    newFileStream = newFileStream,
                    newFileSize = newFileSize
                )
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        resetFileSelection() // Reset stream sau khi thành công
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
        setState { copy(confirmDeleteState = ConfirmationDialogState.Companion.empty()) }
    }

    fun confirmDeleteContent(content: LessonContent) {
        dismissConfirmDeleteDialog()
        deleteContent(content)
    }

    private fun deleteContent(content: LessonContent) {
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
}