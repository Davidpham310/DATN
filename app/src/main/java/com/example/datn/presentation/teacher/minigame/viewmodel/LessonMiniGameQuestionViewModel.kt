package com.example.datn.presentation.teacher.minigame.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.usecase.minigame.MiniGameUseCases
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.minigame.MiniGameQuestionEvent
import com.example.datn.presentation.common.minigame.MiniGameQuestionState
import com.example.datn.core.utils.validation.rules.minigame.ValidateQuestionContent
import com.example.datn.core.utils.validation.rules.minigame.ValidateQuestionOrder
import com.example.datn.core.utils.validation.rules.minigame.ValidateQuestionScore
import com.example.datn.core.utils.validation.rules.minigame.ValidateQuestionTimeLimit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LessonMiniGameQuestionViewModel @Inject constructor(
    private val miniGameUseCases: MiniGameUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<MiniGameQuestionState, MiniGameQuestionEvent>(
    MiniGameQuestionState(), notificationManager
) {

    private val questionContentValidator = ValidateQuestionContent()
    private val questionScoreValidator = ValidateQuestionScore()
    private val questionTimeLimitValidator = ValidateQuestionTimeLimit()
    private val questionOrderValidator = ValidateQuestionOrder()

    private var currentGameId: String = ""

    fun setGameId(gameId: String) {
        currentGameId = gameId
        setState { copy(currentGameId = gameId) }
        loadQuestions(gameId)
    }

    // =====================================================
    // EVENT HANDLING
    // =====================================================
    override fun onEvent(event: MiniGameQuestionEvent) {
        when (event) {
            is MiniGameQuestionEvent.LoadQuestionsForGame -> loadQuestions(event.gameId)
            is MiniGameQuestionEvent.RefreshQuestions -> refreshQuestions()
            is MiniGameQuestionEvent.SelectQuestion -> setState { copy(selectedQuestion = event.question) }

            is MiniGameQuestionEvent.ShowAddQuestionDialog -> {
                setState { copy(showAddEditDialog = true, editingQuestion = null) }
            }
            is MiniGameQuestionEvent.EditQuestion -> {
                setState { copy(showAddEditDialog = true, editingQuestion = event.question) }
            }
            is MiniGameQuestionEvent.DeleteQuestion -> showConfirmDeleteQuestion(event.question)
            is MiniGameQuestionEvent.DismissDialog -> {
                setState { copy(showAddEditDialog = false, editingQuestion = null) }
            }

            is MiniGameQuestionEvent.ConfirmAddQuestion -> addQuestion(event)
            is MiniGameQuestionEvent.ConfirmEditQuestion -> updateQuestion(event)
        }
    }

    // =====================================================
    // CRUD & LOAD QUESTIONS
    // =====================================================
    private fun loadQuestions(gameId: String) {
        setState { copy(currentGameId = gameId) }
        viewModelScope.launch {
            // Load game information first
            miniGameUseCases.getMiniGameById(gameId).collect { gameResult ->
                when (gameResult) {
                    is Resource.Success -> {
                        setState { copy(currentGame = gameResult.data) }
                    }
                    is Resource.Error -> {
                        Log.e("LessonMiniGameQuestionVM", "Failed to load game: ${gameResult.message}")
                    }
                    is Resource.Loading -> { /* No action needed */ }
                }
            }
            
            // Load questions
            miniGameUseCases.getQuestionsByMiniGame(gameId).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        val questions = result.data ?: emptyList()
                        setState {
                            copy(
                                questions = questions,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(result.message ?: "Tải câu hỏi thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun refreshQuestions() {
        val gameId = state.value.currentGameId
        if (gameId.isNotEmpty()) loadQuestions(gameId)
    }

    private fun addQuestion(event: MiniGameQuestionEvent.ConfirmAddQuestion) {
        val contentResult = questionContentValidator.validate(event.content)
        if (!contentResult.successful) {
            showNotification(contentResult.errorMessage ?: "Nội dung câu hỏi không hợp lệ", NotificationType.ERROR)
            return
        }

        val scoreResult = questionScoreValidator.validate(event.score)
        if (!scoreResult.successful) {
            showNotification(scoreResult.errorMessage ?: "Điểm số không hợp lệ", NotificationType.ERROR)
            return
        }

        val timeResult = questionTimeLimitValidator.validate(event.timeLimit)
        if (!timeResult.successful) {
            showNotification(timeResult.errorMessage ?: "Thời gian không hợp lệ", NotificationType.ERROR)
            return
        }

        val orderResult = questionOrderValidator.validate(event.order)
        if (!orderResult.successful) {
            showNotification(orderResult.errorMessage ?: "Thứ tự (order) không hợp lệ", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val newQuestion = MiniGameQuestion(
                id = "",
                miniGameId = event.gameId,
                content = event.content.trim(),
                questionType = event.questionType,
                score = event.score,
                timeLimit = event.timeLimit,
                order = event.order,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            miniGameUseCases.createQuestion(newQuestion).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false) }
                        showNotification("Thêm câu hỏi thành công!", NotificationType.SUCCESS)
                        refreshQuestions()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Thêm câu hỏi thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    private fun updateQuestion(event: MiniGameQuestionEvent.ConfirmEditQuestion) {
        val contentResult = questionContentValidator.validate(event.content)
        if (!contentResult.successful) {
            showNotification(contentResult.errorMessage ?: "Nội dung câu hỏi không hợp lệ", NotificationType.ERROR)
            return
        }

        val scoreResult = questionScoreValidator.validate(event.score)
        if (!scoreResult.successful) {
            showNotification(scoreResult.errorMessage ?: "Điểm số không hợp lệ", NotificationType.ERROR)
            return
        }

        val timeResult = questionTimeLimitValidator.validate(event.timeLimit)
        if (!timeResult.successful) {
            showNotification(timeResult.errorMessage ?: "Thời gian không hợp lệ", NotificationType.ERROR)
            return
        }

        val orderResult = questionOrderValidator.validate(event.order)
        if (!orderResult.successful) {
            showNotification(orderResult.errorMessage ?: "Thứ tự (order) không hợp lệ", NotificationType.ERROR)
            return
        }

        val originalQuestion = state.value.questions.find { it.id == event.id }
            ?: state.value.editingQuestion

        if (originalQuestion != null && originalQuestion.questionType != event.questionType) {
            showNotification("Không thể thay đổi loại câu hỏi sau khi tạo", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            val updatedQuestion = MiniGameQuestion(
                id = event.id,
                miniGameId = event.gameId,
                content = event.content.trim(),
                questionType = originalQuestion?.questionType ?: event.questionType,
                score = event.score,
                timeLimit = event.timeLimit,
                order = event.order,
                createdAt = originalQuestion?.createdAt ?: Instant.now(),
                updatedAt = Instant.now()
            )

            miniGameUseCases.updateQuestion(updatedQuestion).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false, showAddEditDialog = false, editingQuestion = null) }
                        showNotification("Cập nhật câu hỏi thành công!", NotificationType.SUCCESS)
                        refreshQuestions()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Cập nhật câu hỏi thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }

    // =====================================================
    // DELETE QUESTION
    // =====================================================
    private fun showConfirmDeleteQuestion(question: MiniGameQuestion) {
        setState {
            copy(
                confirmDeleteState = ConfirmationDialogState(
                    isShowing = true,
                    title = "Xác nhận xóa câu hỏi",
                    message = "Bạn có chắc chắn muốn xóa câu hỏi này?\nHành động này không thể hoàn tác.",
                    data = question
                )
            )
        }
    }

    fun dismissConfirmDeleteDialog() {
        setState { copy(confirmDeleteState = ConfirmationDialogState.empty()) }
    }

    fun confirmDeleteQuestion(question: MiniGameQuestion) {
        dismissConfirmDeleteDialog()
        viewModelScope.launch {
            miniGameUseCases.deleteQuestion(question.id).collect { result ->
                when (result) {
                    is Resource.Loading -> setState { copy(isLoading = true) }
                    is Resource.Success -> {
                        setState { copy(isLoading = false) }
                        showNotification("Xóa câu hỏi thành công!", NotificationType.SUCCESS, 3000L)
                        refreshQuestions()
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(result.message ?: "Xóa câu hỏi thất bại", NotificationType.ERROR)
                    }
                }
            }
        }
    }
}
