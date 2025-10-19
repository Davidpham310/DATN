package com.example.datn.presentation.teacher.lessons

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.lesson.CreateLessonParams
import com.example.datn.domain.usecase.lesson.LessonUseCases
import com.example.datn.domain.usecase.lesson.UpdateLessonParams
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.lesson.LessonManagerEvent
import com.example.datn.presentation.common.lesson.LessonManagerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonManagerViewModel @Inject constructor(
    private val lessonUseCases: LessonUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<LessonManagerState, LessonManagerEvent>(LessonManagerState(), notificationManager) {

    private var currentTeacherId: String = ""

    init {
        viewModelScope.launch {
            authUseCases.getCurrentIdUser.invoke()
                .distinctUntilChanged()
                .collect { id ->
                    currentTeacherId = id
                    Log.d("LessonManagerVM", "Loaded teacherId: $currentTeacherId")
                }
        }
    }

    override fun onEvent(event: LessonManagerEvent) {
        when (event) {
            is LessonManagerEvent.LoadLessonsForClass -> loadLessons(event.classId)
            is LessonManagerEvent.RefreshLessons -> refreshLessons()
            is LessonManagerEvent.SelectLesson -> setState { copy(selectedLesson = event.lesson) }
            is LessonManagerEvent.ShowAddLessonDialog -> setState {
                copy(showAddEditDialog = true, editingLesson = null)
            }
            is LessonManagerEvent.EditLesson -> setState {
                copy(showAddEditDialog = true, editingLesson = event.lesson)
            }
            is LessonManagerEvent.DeleteLesson -> showConfirmDeleteLesson(event.lesson)
            is LessonManagerEvent.DismissDialog -> setState {
                copy(showAddEditDialog = false, editingLesson = null)
            }
            is LessonManagerEvent.ConfirmAddLesson -> addLesson(
                event.classId, event.title, event.description, event.contentLink, event.order
            )
            is LessonManagerEvent.ConfirmEditLesson -> updateLesson(
                event.id, event.classId, event.title, event.description, event.contentLink, event.order
            )
            is LessonManagerEvent.LoadLessonContent -> loadLessonContent(event.lessonId)
            is LessonManagerEvent.AddContent -> addContent(event.content)
            is LessonManagerEvent.UpdateContent -> updateContent(event.content)
            is LessonManagerEvent.DeleteContent -> deleteContent(event.contentId)
        }
    }

    private fun loadLessons(classId: String) {
        setState { copy(currentClassId = classId) }
        viewModelScope.launch {
            lessonUseCases.getLessonsByClass(classId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> setState {
                        copy(lessons = result.data ?: emptyList(), isLoading = false, error = null)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải bài học thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun refreshLessons() {
        val classId = state.value.currentClassId
        if (classId.isNotEmpty()) {
            loadLessons(classId)
        }
    }

    private fun addLesson(
        classId: String,
        title: String,
        description: String?,
        contentLink: String?,
        order: Int
    ) {
        if (currentTeacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }

        if (title.isBlank()) {
            showNotification("Tiêu đề bài học không được để trống", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            lessonUseCases.createLesson(
                CreateLessonParams(classId, currentTeacherId, title, description, contentLink, order)
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false) }
                        showNotification("Thêm bài học thành công!", NotificationType.SUCCESS)
                        refreshLessons()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Thêm bài học thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun updateLesson(
        id: String,
        classId: String,
        title: String,
        description: String?,
        contentLink: String?,
        order: Int
    ) {
        if (currentTeacherId.isBlank()) {
            showNotification("Không xác định được giáo viên", NotificationType.ERROR)
            return
        }

        if (title.isBlank()) {
            showNotification("Tiêu đề bài học không được để trống", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            lessonUseCases.updateLesson(
                UpdateLessonParams(id, classId, currentTeacherId, title, description, contentLink, order)
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false, editingLesson = null) }
                        showNotification("Cập nhật bài học thành công!", NotificationType.SUCCESS)
                        refreshLessons()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Cập nhật bài học thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun showConfirmDeleteLesson(lesson: Lesson) {
        setState {
            copy(
                confirmDeleteState = ConfirmationDialogState(
                    isShowing = true,
                    title = "Xác nhận xóa bài học",
                    message = "Bạn có chắc chắn muốn xóa bài học \"${lesson.title}\"?\n\nHành động này sẽ xóa toàn bộ nội dung liên quan và không thể hoàn tác.",
                    data = lesson
                )
            )
        }
    }

    fun dismissConfirmDeleteDialog() {
        setState { copy(confirmDeleteState = ConfirmationDialogState.Companion.empty()) }
    }

    fun confirmDeleteLesson(lesson: Lesson) {
        dismissConfirmDeleteDialog()
        deleteLesson(lesson)
    }

    private fun deleteLesson(lesson: Lesson) {
        viewModelScope.launch {
            lessonUseCases.deleteLesson(lesson.id).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        showNotification("Xóa bài học thành công!", NotificationType.SUCCESS)
                        refreshLessons()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Xóa bài học thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun loadLessonContent(lessonId: String) {
        viewModelScope.launch {
            lessonUseCases.getLessonContent(lessonId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> setState {
                        copy(lessonContents = result.data ?: emptyList(), isLoading = false)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Tải nội dung thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun addContent(content: LessonContent) {
        // Implementation for adding lesson content
        showNotification("Chức năng đang phát triển", NotificationType.INFO)
    }

    private fun updateContent(content: LessonContent) {
        viewModelScope.launch {
            lessonUseCases.updateLessonContent(content).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        showNotification("Cập nhật nội dung thành công!", NotificationType.SUCCESS)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Cập nhật nội dung thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun deleteContent(contentId: String) {
        // Implementation for deleting lesson content
        showNotification("Chức năng đang phát triển", NotificationType.INFO)
    }
}