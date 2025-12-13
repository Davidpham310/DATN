package com.example.datn.presentation.teacher.test.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseViewModel
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.usecase.test.TestQuestionUseCases
import com.example.datn.presentation.common.dialogs.ConfirmationDialogState
import com.example.datn.presentation.common.test.TestQuestionEvent
import com.example.datn.presentation.common.test.TestQuestionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject


@HiltViewModel
class TestQuestionManagerViewModel @Inject constructor(
    private val testQuestionUseCases: TestQuestionUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<TestQuestionState, TestQuestionEvent>(TestQuestionState(), notificationManager) {

    override fun onEvent(event: TestQuestionEvent) {
        when (event) {
            is TestQuestionEvent.LoadQuestions -> load(event.testId)
            TestQuestionEvent.RefreshQuestions -> refresh()
            TestQuestionEvent.ShowAddQuestionDialog -> showAddDialog()
            is TestQuestionEvent.EditQuestion -> showEditDialog(event.question)
            is TestQuestionEvent.DeleteQuestion -> showConfirmDelete(event.question)
            TestQuestionEvent.DismissDialog -> dismissDialog()
            is TestQuestionEvent.ConfirmAddQuestion -> addQuestion(
                event.testId, event.content, event.score, event.questionType, event.mediaUrl
            )
            is TestQuestionEvent.ConfirmEditQuestion -> updateQuestion(
                event.id, event.testId, event.content, event.score, event.questionType, event.mediaUrl
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
                    is Resource.Success -> setState { 
                        copy(isLoading = false, questions = result.data ?: emptyList(), error = null) 
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
                            showNotification("Xóa câu hỏi thành công", NotificationType.SUCCESS)
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
        questionType: QuestionType,
        mediaUrl: String?
    ) {
        viewModelScope.launch {
            val maxOrder = state.value.questions.maxOfOrNull { it.order } ?: 0
            val newQuestion = TestQuestion(
                id = "",
                testId = testId,
                content = content,
                score = score,
                questionType = questionType,
                mediaUrl = mediaUrl,
                order = maxOrder + 1,
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
        questionType: QuestionType,
        mediaUrl: String?
    ) {
        viewModelScope.launch {
            val existing = state.value.editingQuestion ?: return@launch
            val updated = existing.copy(
                content = content,
                score = score,
                questionType = questionType,
                mediaUrl = mediaUrl,
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
}


