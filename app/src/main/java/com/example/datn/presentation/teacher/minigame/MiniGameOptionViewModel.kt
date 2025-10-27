package com.example.datn.presentation.teacher.minigame

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.MiniGameOption
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

            val newOption = MiniGameOption(
                id = "",
                miniGameQuestionId = event.questionId,
                content = event.content.trim(),
                isCorrect = event.isCorrect,
                order = newOrder,
                mediaUrl = event.mediaUrl?.trim(),
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
                        showNotification(result.message