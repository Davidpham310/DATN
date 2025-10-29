package com.example.datn.presentation.teacher.test

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.usecase.test.TestOptionUseCases
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.test.TestOptionEvent
import com.example.datn.presentation.common.test.TestOptionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TestOptionViewModel @Inject constructor(
    private val useCases: TestOptionUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<TestOptionState, TestOptionEvent>(TestOptionState(), notificationManager) {

    fun setQuestionId(questionId: String) {
        setState { copy(currentQuestionId = questionId) }
        loadOptions(questionId)
    }

    override fun onEvent(event: TestOptionEvent) {
        when (event) {
            is TestOptionEvent.LoadOptionsForQuestion -> loadOptions(event.questionId)
            is TestOptionEvent.RefreshOptions -> refreshOptions()
            is TestOptionEvent.SelectOption -> setState { copy(selectedOption = event.option) }

            is TestOptionEvent.ShowAddOptionDialog -> setState { copy(showAddEditDialog = true, editingOption = null) }
            is TestOptionEvent.EditOption -> setState { copy(showAddEditDialog = true, editingOption = event.option) }
            is TestOptionEvent.DeleteOption -> showConfirmDeleteOption(event.option)
            is TestOptionEvent.DismissDialog -> setState { copy(showAddEditDialog = false, editingOption = null) }

            is TestOptionEvent.ConfirmAddOption -> addOption(event)
            is TestOptionEvent.ConfirmEditOption -> updateOption(event)
        }
    }

    private fun loadOptions(questionId: String) {
        setState { copy(currentQuestionId = questionId) }
        viewModelScope.launch {
            useCases.listByQuestion(questionId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> setState { copy(options = result.data ?: emptyList(), isLoading = false, error = null) }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải đáp án thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun refreshOptions() {
        val q = state.value.currentQuestionId
        if (q.isNotEmpty()) loadOptions(q)
    }

    private fun addOption(event: TestOptionEvent.ConfirmAddOption) {
        if (event.content.isBlank()) {
            showNotification("Nội dung đáp án không được để trống", NotificationType.ERROR)
            return
        }
        val current = state.value.options
        val order = if (current.isEmpty()) 0 else current.maxOf { it.order } + 1
        viewModelScope.launch {
            val option = TestOption(
                id = "",
                testQuestionId = event.questionId,
                content = event.content.trim(),
                isCorrect = event.isCorrect,
                order = order,
                mediaUrl = event.mediaUrl?.trim(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            useCases.create(option).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false) }
                        showNotification("Thêm đáp án thành công!", NotificationType.SUCCESS)
                        refreshOptions()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Thêm đáp án thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun updateOption(event: TestOptionEvent.ConfirmEditOption) {
        if (event.content.isBlank()) {
            showNotification("Nội dung đáp án không được để trống", NotificationType.ERROR)
            return
        }
        val origin = state.value.options.find { it.id == event.id }
        viewModelScope.launch {
            val option = TestOption(
                id = event.id,
                testQuestionId = event.questionId,
                content = event.content.trim(),
                isCorrect = event.isCorrect,
                order = origin?.order ?: 0,
                mediaUrl = event.mediaUrl?.trim(),
                createdAt = origin?.createdAt ?: Instant.now(),
                updatedAt = Instant.now()
            )
            useCases.update(option).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false, editingOption = null) }
                        showNotification("Cập nhật đáp án thành công!", NotificationType.SUCCESS)
                        refreshOptions()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Cập nhật đáp án thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun showConfirmDeleteOption(option: TestOption) {
        setState {
            copy(
                confirmDeleteState = ConfirmationDialogState(
                    isShowing = true,
                    title = "Xác nhận xóa đáp án",
                    message = "Bạn có chắc chắn muốn xóa đáp án này?\nHành động này không thể hoàn tác.",
                    data = option
                )
            )
        }
    }

    fun dismissConfirmDeleteDialog() {
        setState { copy(confirmDeleteState = ConfirmationDialogState.empty()) }
    }

    fun confirmDeleteOption(option: TestOption) {
        dismissConfirmDeleteDialog()
        viewModelScope.launch {
            useCases.delete(option.id).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        showNotification("Xóa đáp án thành công!", NotificationType.SUCCESS)
                        refreshOptions()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Xóa đáp án thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }
}


