package com.example.datn.presentation.teacher.test

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.usecase.test.TestOptionUseCases
import com.example.datn.domain.usecase.test.TestQuestionUseCases
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
    private val questionUseCases: TestQuestionUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<TestOptionState, TestOptionEvent>(TestOptionState(), notificationManager) {

    companion object {
        private const val TAG = "TestOptionViewModel"
    }

    fun setQuestionId(questionId: String) {
        Log.d(TAG, "setQuestionId: $questionId")
        setState { copy(currentQuestionId = questionId) }
        loadQuestionInfo(questionId)
        loadOptions(questionId)
    }

    private fun loadQuestionInfo(questionId: String) {
        Log.d(TAG, "loadQuestionInfo: $questionId")
        viewModelScope.launch {
            questionUseCases.getById(questionId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        if (result.data != null) {
                            Log.d(TAG, "Question info loaded: type=${result.data.questionType}")
                            setState { copy(currentQuestionType = result.data.questionType) }
                        } else {
                            Log.w(TAG, "Question data is null")
                        }
                    }
                    is Resource.Error -> Log.e(TAG, "Error loading question info: ${result.message}")
                    is Resource.Loading -> Log.d(TAG, "Loading question info...")
                }
            }
        }
    }

    override fun onEvent(event: TestOptionEvent) {
        Log.d(TAG, "onEvent: ${event::class.simpleName}")
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
        Log.d(TAG, "addOption: content='${event.content}', isCorrect=${event.isCorrect}")
        if (event.content.isBlank()) {
            Log.w(TAG, "Add option failed: content is blank")
            showNotification("Nội dung đáp án không được để trống", NotificationType.ERROR)
            return
        }

        // Validation dựa trên QuestionType
        val questionType = state.value.currentQuestionType
        Log.d(TAG, "Validating option for question type: $questionType")
        if (!validateOptionForQuestionType(questionType, event.isCorrect)) {
            return
        }

        val current = state.value.options
        val order = if (current.isEmpty()) 0 else current.maxOf { it.order } + 1
        viewModelScope.launch {
            // Nếu SINGLE_CHOICE và option mới là correct, cần unset các options cũ
            if (questionType == QuestionType.SINGLE_CHOICE && event.isCorrect) {
                unsetAllCorrectOptions()
            }

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
        Log.d(TAG, "updateOption: id=${event.id}, content='${event.content}', isCorrect=${event.isCorrect}")
        if (event.content.isBlank()) {
            Log.w(TAG, "Update option failed: content is blank")
            showNotification("Nội dung đáp án không được để trống", NotificationType.ERROR)
            return
        }

        // Validation dựa trên QuestionType
        val questionType = state.value.currentQuestionType
        Log.d(TAG, "Validating option for question type: $questionType")
        if (!validateOptionForQuestionType(questionType, event.isCorrect)) {
            return
        }

        val origin = state.value.options.find { it.id == event.id }
        viewModelScope.launch {
            // Nếu SINGLE_CHOICE và option được set là correct, cần unset các options khác
            if (questionType == QuestionType.SINGLE_CHOICE && event.isCorrect && origin?.isCorrect == false) {
                unsetAllCorrectOptions(excludeId = event.id)
            }

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
        Log.d(TAG, "confirmDeleteOption: id=${option.id}, content='${option.content}'")
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

    /**
     * Validate option dựa trên QuestionType
     * - SINGLE_CHOICE/MULTIPLE_CHOICE: Cần có options
     * - FILL_BLANK/ESSAY: Không cần options (có thể warning)
     */
    private fun validateOptionForQuestionType(questionType: QuestionType?, isCorrect: Boolean): Boolean {
        Log.d(TAG, "validateOptionForQuestionType: type=$questionType, isCorrect=$isCorrect")
        when (questionType) {
            QuestionType.FILL_BLANK -> {
                showNotification(
                    "Câu hỏi điền vào chỗ trống thường không cần đáp án trắc nghiệm",
                    NotificationType.INFO
                )
                return true // Vẫn cho phép thêm nếu người dùng muốn
            }
            QuestionType.ESSAY -> {
                showNotification(
                    "Câu hỏi tự luận không cần đáp án trắc nghiệm",
                    NotificationType.INFO
                )
                return true // Vẫn cho phép thêm nếu người dùng muốn
            }
            QuestionType.SINGLE_CHOICE -> {
                // Kiểm tra đã có đáp án đúng chưa
                val hasCorrectOption = state.value.options.any { it.isCorrect }
                Log.d(TAG, "SINGLE_CHOICE: hasCorrectOption=$hasCorrectOption, adding correct=$isCorrect")
                if (isCorrect && hasCorrectOption) {
                    Log.i(TAG, "Will replace existing correct option")
                    showNotification(
                        "Câu hỏi trắc nghiệm đơn chỉ được có 1 đáp án đúng. Đáp án đúng cũ sẽ bị thay thế.",
                        NotificationType.INFO
                    )
                }
                return true
            }
            QuestionType.MULTIPLE_CHOICE -> {
                // Cho phép nhiều đáp án đúng
                return true
            }
            null -> {
                showNotification("Không xác định được loại câu hỏi", NotificationType.ERROR)
                return false
            }
        }
    }

    /**
     * Unset tất cả options đang có isCorrect=true
     * Dùng cho SINGLE_CHOICE khi thêm option mới là correct
     */
    private suspend fun unsetAllCorrectOptions(excludeId: String? = null) {
        val correctOptions = state.value.options.filter { it.isCorrect && it.id != excludeId }
        Log.d(TAG, "unsetAllCorrectOptions: found ${correctOptions.size} options to unset (exclude=$excludeId)")
        correctOptions.forEach { option ->
            Log.d(TAG, "Unsetting correct flag for option: id=${option.id}")
            val updated = option.copy(isCorrect = false, updatedAt = Instant.now())
            useCases.update(updated).collect { /* Silent update */ }
        }
        Log.d(TAG, "Finished unsetting correct options")
    }
}


