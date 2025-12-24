package com.example.datn.presentation.teacher.test.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.rules.test.ValidateTestDisplayOrder
import com.example.datn.core.utils.validation.rules.test.ValidateTestMediaUrl
import com.example.datn.core.utils.validation.rules.test.ValidateTestCorrectOptions
import com.example.datn.core.utils.validation.rules.test.ValidateTestOptionContent
import com.example.datn.core.utils.validation.rules.test.ValidateTestOptionCorrectness
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

    private val optionContentValidator = ValidateTestOptionContent()
    private val displayOrderValidator = ValidateTestDisplayOrder()
    private val optionMediaUrlValidator = ValidateTestMediaUrl()
    private val optionCorrectnessValidator = ValidateTestOptionCorrectness()
    private val correctOptionsValidator = ValidateTestCorrectOptions()

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
                    is Resource.Success -> {
                        val options = (result.data ?: emptyList()).sortedBy { it.order }
                        setState { copy(options = options, isLoading = false, error = null) }
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
        val q = state.value.currentQuestionId
        if (q.isNotEmpty()) loadOptions(q)
    }

    private fun addOption(event: TestOptionEvent.ConfirmAddOption) {
        Log.d(TAG, "addOption: content='${event.content}', isCorrect=${event.isCorrect}")
        val questionType = state.value.currentQuestionType
        val trimmedContent = event.content.trim()
        val trimmedMediaUrl = event.mediaUrl?.trim()?.ifBlank { null }

        val contentResult = optionContentValidator.validate(trimmedContent)
        if (!contentResult.successful) {
            showNotification(contentResult.errorMessage ?: "Nội dung đáp án không hợp lệ", NotificationType.ERROR)
            return
        }

        val mediaUrlResult = optionMediaUrlValidator.validate(trimmedMediaUrl)
        if (!mediaUrlResult.successful) {
            showNotification(mediaUrlResult.errorMessage ?: "Media URL không hợp lệ", NotificationType.ERROR)
            return
        }

        val orderResult = displayOrderValidator.validate(event.order)
        if (!orderResult.successful) {
            showNotification(orderResult.errorMessage ?: "Thứ tự hiển thị không hợp lệ", NotificationType.ERROR)
            return
        }

        val isCorrectForSubmit = when (questionType) {
            QuestionType.FILL_BLANK -> true
            QuestionType.ESSAY -> false
            else -> event.isCorrect
        }

        val correctnessResult = optionCorrectnessValidator.validate(questionType to isCorrectForSubmit)
        if (!correctnessResult.successful) {
            showNotification(correctnessResult.errorMessage ?: "Không thể đánh dấu đáp án đúng", NotificationType.ERROR)
            return
        }

        // Câu hỏi điền vào chỗ trống chỉ cần 1 đáp án đúng
        if (questionType == QuestionType.FILL_BLANK && state.value.options.isNotEmpty()) {
            showNotification(
                "Câu hỏi điền vào chỗ trống chỉ cần 1 đáp án. Vui lòng chỉnh sửa đáp án hiện tại.",
                NotificationType.ERROR
            )
            return
        }

        viewModelScope.launch {
            val option = TestOption(
                id = "",
                testQuestionId = event.questionId,
                content = trimmedContent,
                isCorrect = isCorrectForSubmit,
                order = event.order,
                mediaUrl = trimmedMediaUrl,
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
        val questionType = state.value.currentQuestionType
        val trimmedContent = event.content.trim()
        val trimmedMediaUrl = event.mediaUrl?.trim()?.ifBlank { null }

        val contentResult = optionContentValidator.validate(trimmedContent)
        if (!contentResult.successful) {
            showNotification(contentResult.errorMessage ?: "Nội dung đáp án không hợp lệ", NotificationType.ERROR)
            return
        }

        val mediaUrlResult = optionMediaUrlValidator.validate(trimmedMediaUrl)
        if (!mediaUrlResult.successful) {
            showNotification(mediaUrlResult.errorMessage ?: "Media URL không hợp lệ", NotificationType.ERROR)
            return
        }

        val orderResult = displayOrderValidator.validate(event.order)
        if (!orderResult.successful) {
            showNotification(orderResult.errorMessage ?: "Thứ tự hiển thị không hợp lệ", NotificationType.ERROR)
            return
        }

        val isCorrectForSubmit = when (questionType) {
            QuestionType.FILL_BLANK -> true
            QuestionType.ESSAY -> false
            else -> event.isCorrect
        }

        val correctnessResult = optionCorrectnessValidator.validate(questionType to isCorrectForSubmit)
        if (!correctnessResult.successful) {
            showNotification(correctnessResult.errorMessage ?: "Không thể đánh dấu đáp án đúng", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val option = TestOption(
                id = event.id,
                testQuestionId = event.questionId,
                content = trimmedContent,
                isCorrect = isCorrectForSubmit,
                order = event.order,
                mediaUrl = trimmedMediaUrl,
                createdAt = state.value.options.find { it.id == event.id }?.createdAt ?: Instant.now(),
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

    fun validateOptionsForCurrentQuestion(): Boolean {
        val result = correctOptionsValidator.validate(state.value.currentQuestionType to state.value.options)
        if (!result.successful) {
            showNotification(result.errorMessage ?: "Dữ liệu đáp án không hợp lệ", NotificationType.ERROR)
            return false
        }
        return true
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


