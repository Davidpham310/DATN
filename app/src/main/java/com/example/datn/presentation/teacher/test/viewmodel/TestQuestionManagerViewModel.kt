package com.example.datn.presentation.teacher.test.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseViewModel
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.validation.rules.test.ValidateTestDisplayOrder
import com.example.datn.core.utils.validation.rules.test.ValidateTestMediaUrl
import com.example.datn.core.utils.validation.rules.test.ValidateTestQuestionContent
import com.example.datn.core.utils.validation.rules.test.ValidateTestQuestionScore
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.usecase.test.ImportTestQuestionsFromExcelUseCase
import com.example.datn.domain.usecase.test.TestQuestionUseCases
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.test.TestQuestionEvent
import com.example.datn.presentation.common.test.TestQuestionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject


@HiltViewModel
class TestQuestionManagerViewModel @Inject constructor(
    private val testQuestionUseCases: TestQuestionUseCases,
    private val importTestQuestionsFromExcelUseCase: ImportTestQuestionsFromExcelUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<TestQuestionState, TestQuestionEvent>(TestQuestionState(), notificationManager) {

    private val questionContentValidator = ValidateTestQuestionContent()
    private val questionScoreValidator = ValidateTestQuestionScore()
    private val questionMediaUrlValidator = ValidateTestMediaUrl()
    private val displayOrderValidator = ValidateTestDisplayOrder()

    override fun onEvent(event: TestQuestionEvent) {
        when (event) {
            is TestQuestionEvent.LoadQuestions -> load(event.testId)
            TestQuestionEvent.RefreshQuestions -> refresh()
            TestQuestionEvent.ShowAddQuestionDialog -> showAddDialog()
            is TestQuestionEvent.EditQuestion -> showEditDialog(event.question)
            is TestQuestionEvent.DeleteQuestion -> showConfirmDelete(event.question)
            TestQuestionEvent.DismissDialog -> dismissDialog()
            is TestQuestionEvent.ConfirmAddQuestion -> addQuestion(
                event.testId,
                event.content,
                event.score,
                event.timeLimit,
                event.order,
                event.questionType,
                event.mediaUrl
            )
            is TestQuestionEvent.ConfirmEditQuestion -> updateQuestion(
                event.id,
                event.testId,
                event.content,
                event.score,
                event.timeLimit,
                event.order,
                event.questionType,
                event.mediaUrl
            )
            is TestQuestionEvent.SelectQuestion -> {}
        }
    }

    fun setTest(testId: String, testTitle: String) {
        setState { copy(testId = testId, testTitle = testTitle) }
        load(testId)
    }

    private fun load(testId: String) {
        testQuestionUseCases.listByTest(testId)
            .onEach { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        val questions = (result.data ?: emptyList()).sortedBy { it.order }
                        setState {
                            copy(isLoading = false, questions = questions, error = null)
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải câu hỏi thất bại", NotificationType.ERROR)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun showAddDialog() {
        setState { copy(showAddEditDialog = true, editingQuestion = null) }
    }

    private fun showEditDialog(question: TestQuestion) {
        setState { copy(showAddEditDialog = true, editingQuestion = question) }
    }

    private fun dismissDialog() {
        setState { copy(showAddEditDialog = false, editingQuestion = null) }
    }

    private fun showConfirmDelete(question: TestQuestion) {
        setState {
            copy(
                confirmDeleteState = confirmDeleteState.copy(
                    isShowing = true,
                    data = question
                )
            )
        }
    }

    fun dismissConfirmDeleteDialog() {
        setState { copy(confirmDeleteState = confirmDeleteState.copy(isShowing = false, data = null)) }
    }

    fun confirmDeleteQuestion(question: TestQuestion) {
        viewModelScope.launch {
            testQuestionUseCases.delete(question.id)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { 
                                copy(
                                    isLoading = false,
                                    confirmDeleteState = confirmDeleteState.copy(isShowing = false, data = null)
                                ) 
                            }
                            showNotification("Xóa câu hỏi thành công", NotificationType.SUCCESS, 3000L)
                            refresh()
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            showNotification(result.message ?: "Xóa câu hỏi thất bại", NotificationType.ERROR)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun addQuestion(
        testId: String,
        content: String,
        score: Double,
        timeLimit: Int,
        order: Int,
        questionType: QuestionType,
        mediaUrl: String?
    ) {
        val trimmedContent = content.trim()
        val trimmedMediaUrl = mediaUrl?.trim()?.ifBlank { null }

        val contentResult = questionContentValidator.validate(trimmedContent)
        if (!contentResult.successful) {
            showNotification(contentResult.errorMessage ?: "Nội dung câu hỏi không hợp lệ", NotificationType.ERROR)
            return
        }

        val scoreResult = questionScoreValidator.validate(score)
        if (!scoreResult.successful) {
            showNotification(scoreResult.errorMessage ?: "Điểm số không hợp lệ", NotificationType.ERROR)
            return
        }

        val mediaUrlResult = questionMediaUrlValidator.validate(trimmedMediaUrl)
        if (!mediaUrlResult.successful) {
            showNotification(mediaUrlResult.errorMessage ?: "Media URL không hợp lệ", NotificationType.ERROR)
            return
        }

        val timeLimitResult = displayOrderValidator.validate(timeLimit)
        if (!timeLimitResult.successful) {
            showNotification(timeLimitResult.errorMessage ?: "Thời gian trả lời không hợp lệ", NotificationType.ERROR)
            return
        }

        val orderResult = displayOrderValidator.validate(order)
        if (!orderResult.successful) {
            showNotification(orderResult.errorMessage ?: "Thứ tự hiển thị không hợp lệ", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val newQuestion = TestQuestion(
                id = "",
                testId = testId,
                content = trimmedContent,
                score = score,
                questionType = questionType,
                mediaUrl = trimmedMediaUrl,
                timeLimit = timeLimit,
                order = order,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            testQuestionUseCases.create(newQuestion)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { copy(isLoading = false, showAddEditDialog = false) }
                            showNotification("Thêm câu hỏi thành công", NotificationType.SUCCESS)
                            refresh()
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            showNotification(result.message ?: "Thêm câu hỏi thất bại", NotificationType.ERROR)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun updateQuestion(
        id: String,
        testId: String,
        content: String,
        score: Double,
        timeLimit: Int,
        order: Int,
        questionType: QuestionType,
        mediaUrl: String?
    ) {
        val existing = state.value.editingQuestion
        if (existing == null) {
            showNotification("Không tìm thấy câu hỏi cần chỉnh sửa", NotificationType.ERROR)
            return
        }

        if (existing.questionType != questionType) {
            showNotification("Không thể thay đổi loại câu hỏi sau khi tạo", NotificationType.ERROR)
            return
        }

        val trimmedContent = content.trim()
        val trimmedMediaUrl = mediaUrl?.trim()?.ifBlank { null }

        val contentResult = questionContentValidator.validate(trimmedContent)
        if (!contentResult.successful) {
            showNotification(contentResult.errorMessage ?: "Nội dung câu hỏi không hợp lệ", NotificationType.ERROR)
            return
        }

        val scoreResult = questionScoreValidator.validate(score)
        if (!scoreResult.successful) {
            showNotification(scoreResult.errorMessage ?: "Điểm số không hợp lệ", NotificationType.ERROR)
            return
        }

        val mediaUrlResult = questionMediaUrlValidator.validate(trimmedMediaUrl)
        if (!mediaUrlResult.successful) {
            showNotification(mediaUrlResult.errorMessage ?: "Media URL không hợp lệ", NotificationType.ERROR)
            return
        }

        val timeLimitResult = displayOrderValidator.validate(timeLimit)
        if (!timeLimitResult.successful) {
            showNotification(timeLimitResult.errorMessage ?: "Thời gian trả lời không hợp lệ", NotificationType.ERROR)
            return
        }

        val orderResult = displayOrderValidator.validate(order)
        if (!orderResult.successful) {
            showNotification(orderResult.errorMessage ?: "Thứ tự hiển thị không hợp lệ", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val updated = existing.copy(
                content = trimmedContent,
                score = score,
                questionType = existing.questionType,
                mediaUrl = trimmedMediaUrl,
                timeLimit = timeLimit,
                order = order,
                updatedAt = Instant.now()
            )

            testQuestionUseCases.update(updated)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> setState { copy(isLoading = true) }
                        is Resource.Success -> {
                            setState { copy(isLoading = false, showAddEditDialog = false, editingQuestion = null) }
                            showNotification("Cập nhật câu hỏi thành công", NotificationType.SUCCESS)
                            refresh()
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false) }
                            showNotification(result.message ?: "Cập nhật câu hỏi thất bại", NotificationType.ERROR)
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun refresh() {
        val id = state.value.testId
        if (id.isNotEmpty()) load(id)
    }

    fun importFromExcel(testId: String, inputStream: InputStream) {
        val existingMaxOrder = state.value.questions.maxOfOrNull { it.order } ?: 0
        val startingOrder = existingMaxOrder + 1

        importTestQuestionsFromExcelUseCase(testId, inputStream, startingOrder)
            .onEach { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        val summary = result.data
                        if (summary != null) {
                            showNotification(
                                "Import xong: ${summary.importedQuestions} câu hỏi, ${summary.importedOptions} đáp án. Bỏ qua ${summary.skippedRows}/${summary.totalRows} dòng.",
                                NotificationType.SUCCESS
                            )
                        } else {
                            showNotification("Import xong", NotificationType.SUCCESS)
                        }
                        refresh()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Import thất bại", NotificationType.ERROR)
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}


