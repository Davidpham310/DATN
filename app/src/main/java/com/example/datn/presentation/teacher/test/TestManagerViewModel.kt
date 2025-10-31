package com.example.datn.presentation.teacher.test

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Test
import com.example.datn.domain.usecase.test.TestUseCases
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.test.TestEvent
import com.example.datn.presentation.common.test.TestState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TestManagerViewModel @Inject constructor(
    private val testUseCases: TestUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<TestState, TestEvent>(TestState(), notificationManager) {

    override fun onEvent(event: TestEvent) {
        when (event) {
            is TestEvent.LoadTests -> loadTests(event.lessonId)
            is TestEvent.RefreshTests -> refreshTests()
            is TestEvent.SelectTest -> setState { copy(selectedTest = event.test) }
            is TestEvent.ShowAddTestDialog -> setState {
                copy(showAddEditDialog = true, editingTest = null)
            }
            is TestEvent.EditTest -> setState {
                copy(showAddEditDialog = true, editingTest = event.test)
            }
            is TestEvent.DeleteTest -> showConfirmDeleteTest(event.test)
            is TestEvent.DismissDialog -> setState {
                copy(showAddEditDialog = false, editingTest = null)
            }
            is TestEvent.ConfirmAddTest -> addTest(
                event.classId,
                event.lessonId,
                event.title,
                event.description,
                event.totalScore,
                event.startTime,
                event.endTime
            )
            is TestEvent.ConfirmEditTest -> updateTest(
                event.id,
                event.classId,
                event.lessonId,
                event.title,
                event.description,
                event.totalScore,
                event.startTime,
                event.endTime
            )
        }
    }

    private fun loadTests(lessonId: String?) {
        if (lessonId == null) return
        
        viewModelScope.launch {
            testUseCases.listByLesson(lessonId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> setState {
                        copy(isLoading = false, tests = result.data ?: emptyList(), error = null)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải danh sách bài kiểm tra thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun refreshTests() {
        // Reload current lesson's tests if available
        val currentTest = state.value.selectedTest
        if (currentTest != null) {
            loadTests(currentTest.lessonId)
        }
    }

    private fun addTest(
        classId: String,
        lessonId: String,
        title: String,
        description: String?,
        totalScore: Double,
        startTime: Instant,
        endTime: Instant
    ) {
        if (title.isBlank()) {
            showNotification("Tiêu đề bài kiểm tra không được để trống", NotificationType.ERROR)
            return
        }

        if (totalScore <= 0) {
            showNotification("Tổng điểm phải lớn hơn 0", NotificationType.ERROR)
            return
        }

        if (startTime.isAfter(endTime)) {
            showNotification("Thời gian bắt đầu phải trước thời gian kết thúc", NotificationType.ERROR)
            return
        }

        val now = Instant.now()
        val test = Test(
            id = "",
            classId = classId,
            lessonId = lessonId,
            title = title,
            description = description,
            totalScore = totalScore,
            startTime = startTime,
            endTime = endTime,
            createdAt = now,
            updatedAt = now
        )

        viewModelScope.launch {
            testUseCases.createTest(test).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false) }
                        showNotification("Thêm bài kiểm tra thành công!", NotificationType.SUCCESS)
                        loadTests(lessonId)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Thêm bài kiểm tra thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun updateTest(
        id: String,
        classId: String,
        lessonId: String,
        title: String,
        description: String?,
        totalScore: Double,
        startTime: Instant,
        endTime: Instant
    ) {
        if (title.isBlank()) {
            showNotification("Tiêu đề bài kiểm tra không được để trống", NotificationType.ERROR)
            return
        }

        if (totalScore <= 0) {
            showNotification("Tổng điểm phải lớn hơn 0", NotificationType.ERROR)
            return
        }

        if (startTime.isAfter(endTime)) {
            showNotification("Thời gian bắt đầu phải trước thời gian kết thúc", NotificationType.ERROR)
            return
        }

        val editingTest = state.value.editingTest
        if (editingTest == null) {
            showNotification("Không tìm thấy bài kiểm tra cần chỉnh sửa", NotificationType.ERROR)
            return
        }

        val test = editingTest.copy(
            classId = classId,
            lessonId = lessonId,
            title = title,
            description = description,
            totalScore = totalScore,
            startTime = startTime,
            endTime = endTime,
            updatedAt = Instant.now()
        )

        viewModelScope.launch {
            testUseCases.updateTest(test).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false, editingTest = null) }
                        showNotification("Cập nhật bài kiểm tra thành công!", NotificationType.SUCCESS)
                        loadTests(lessonId)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Cập nhật bài kiểm tra thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun showConfirmDeleteTest(test: Test) {
        setState {
            copy(
                confirmDeleteState = ConfirmationDialogState(
                    isShowing = true,
                    title = "Xác nhận xóa bài kiểm tra",
                    message = "Bạn có chắc chắn muốn xóa bài kiểm tra \"${test.title}\"?\n\nHành động này sẽ xóa toàn bộ câu hỏi và kết quả liên quan và không thể hoàn tác.",
                    data = test
                )
            )
        }
    }

    fun dismissConfirmDeleteDialog() {
        setState { copy(confirmDeleteState = ConfirmationDialogState.empty()) }
    }

    fun confirmDeleteTest(test: Test) {
        dismissConfirmDeleteDialog()
        deleteTest(test)
    }

    private fun deleteTest(test: Test) {
        viewModelScope.launch {
            testUseCases.deleteTest(test.id).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        showNotification("Xóa bài kiểm tra thành công!", NotificationType.SUCCESS)
                        loadTests(test.lessonId)
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Xóa bài kiểm tra thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }
}
