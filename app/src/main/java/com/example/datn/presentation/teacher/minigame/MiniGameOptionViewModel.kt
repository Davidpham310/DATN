package com.example.datn.presentation.teacher.minigame

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.usecase.minigame.MiniGameUseCases
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.minigame.MiniGameOptionEvent
import com.example.datn.presentation.common.minigame.MiniGameOptionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MiniGameOptionViewModel @Inject constructor(
    private val miniGameUseCases: MiniGameUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<MiniGameOptionState, MiniGameOptionEvent>(
    MiniGameOptionState(), notificationManager
) {

    fun setQuestionId(questionId: String) {
        setState { copy(currentQuestionId = questionId) }
        loadOptions(questionId)
    }

    // =====================================================
    // EVENT HANDLING
    // =====================================================
    override fun onEvent(event: MiniGameOptionEvent) {
        when (event) {
            is MiniGameOptionEvent.LoadOptionsForQuestion -> loadOptions(event.questionId)
            is MiniGameOptionEvent.RefreshOptions -> refreshOptions()
            is MiniGameOptionEvent.SelectOption -> setState { copy(selectedOption = event.option) }

            is MiniGameOptionEvent.ShowAddOptionDialog -> {
                setState { copy(showAddEditDialog = true, editingOption = null) }
            }
            is MiniGameOptionEvent.EditOption -> {
                setState { copy(showAddEditDialog = true, editingOption = event.option) }
            }
            is MiniGameOptionEvent.DeleteOption -> showConfirmDeleteOption(event.option)
            is MiniGameOptionEvent.DismissDialog -> {
                setState { copy(showAddEditDialog = false, editingOption = null) }
            }

            is MiniGameOptionEvent.ConfirmAddOption -> addOption(event)
            is MiniGameOptionEvent.ConfirmEditOption -> updateOption(event)
        }
    }

    // =====================================================
    // CRUD & LOAD OPTIONS
    // =====================================================
    private fun loadOptions(questionId: String) {
        setState { copy(currentQuestionId = questionId) }
        viewModelScope.launch {
            // Load options
            miniGameUseCases.getOptionsByQuestion(questionId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        val options = result.data ?: emptyList()
                        setState {
                            copy(
                                options = options,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải đáp án thất bại", NotificationType.ERROR)
                    }
                }
            }
        }

        // Load question meta (type and game) in parallel
        viewModelScope.launch {
            miniGameUseCases.getQuestionById(questionId).collect { r ->
                when (r) {
                    is Resource.Success -> {
                        val question = r.data
                        setState { copy(currentQuestionType = question?.questionType) }
                        
                        // Load game to get gameType
                        question?.miniGameId?.let { gameId ->
                            miniGameUseCases.getMiniGameById(gameId).collect { gameResult ->
                                when (gameResult) {
                                    is Resource.Success -> setState { copy(currentGameType = gameResult.data?.gameType) }
                                    else -> {}
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun refreshOptions() {
        val questionId = state.value.currentQuestionId
        if (questionId.isNotEmpty()) loadOptions(questionId)
    }

    private fun addOption(event: MiniGameOptionEvent.ConfirmAddOption) {
        if (event.content.isBlank()) {
            showNotification("Nội dung đáp án không được để trống", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            // Tính order dựa trên số lượng đáp án hiện có
            val currentOptions = state.value.options
            val newOrder = if (currentOptions.isEmpty()) 0 else currentOptions.maxOf { it.order } + 1

            // Enforce single-correct for non-multiple-choice types
            val qType = state.value.currentQuestionType
            if (event.isCorrect && qType != QuestionType.MULTIPLE_CHOICE) {
                val hasCorrect = currentOptions.any { it.isCorrect }
                if (hasCorrect) {
                    showNotification("Câu hỏi này chỉ được phép có một đáp án đúng.", NotificationType.ERROR)
                    return@launch
                }
            }

            val newOption = MiniGameOption(
                id = "",
                miniGameQuestionId = event.questionId,
                content = event.content.trim(),
                isCorrect = event.isCorrect,
                order = newOrder,
                mediaUrl = event.mediaUrl?.trim(),
                hint = event.hint?.trim(),
                pairContent = event.pairContent?.trim(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            miniGameUseCases.createOption(newOption).collect { result ->
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

    private fun updateOption(event: MiniGameOptionEvent.ConfirmEditOption) {
        if (event.content.isBlank()) {
            showNotification("Nội dung đáp án không được để trống", NotificationType.ERROR)
            return
        }

        val original = state.value.options.find { it.id == event.id }

        viewModelScope.launch {
            // Enforce single-correct for non-multiple-choice types
            val qType = state.value.currentQuestionType
            if (event.isCorrect && qType != QuestionType.MULTIPLE_CHOICE) {
                val anotherCorrect = state.value.options.any { it.isCorrect && it.id != event.id }
                if (anotherCorrect) {
                    showNotification("Câu hỏi này chỉ được phép có một đáp án đúng.", NotificationType.ERROR)
                    return@launch
                }
            }

            val updatedOption = MiniGameOption(
                id = event.id,
                miniGameQuestionId = event.questionId,
                content = event.content.trim(),
                isCorrect = event.isCorrect,
                order = original?.order ?: 0,
                mediaUrl = event.mediaUrl?.trim(),
                hint = event.hint?.trim(),
                pairContent = event.pairContent?.trim(),
                createdAt = original?.createdAt ?: Instant.now(),
                updatedAt = Instant.now()
            )

            miniGameUseCases.updateOption(updatedOption).collect { result ->
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

    // =====================================================
    // DELETE OPTION
    // =====================================================
    private fun showConfirmDeleteOption(option: MiniGameOption) {
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

    fun confirmDeleteOption(option: MiniGameOption) {
        dismissConfirmDeleteDialog()
        viewModelScope.launch {
            miniGameUseCases.deleteOption(option.id).collect { result ->
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