package com.example.datn.presentation.teacher.test

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseEvent
import com.example.datn.core.base.BaseState
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.presentation.notifications.NotificationManager
import com.example.datn.core.presentation.notifications.NotificationType
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestQuestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestQuestionState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val testId: String = "",
    val testTitle: String = "",
    val questions: List<TestQuestion> = emptyList()
): BaseState

sealed class TestQuestionEvent : BaseEvent {
    data class Load(val testId: String) : TestQuestionEvent()
    object Refresh : TestQuestionEvent()
}

@HiltViewModel
class TestQuestionManagerViewModel @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    notificationManager: NotificationManager
) : BaseViewModel<TestQuestionState, TestQuestionEvent>(TestQuestionState(), notificationManager) {

    override fun onEvent(event: TestQuestionEvent) {
        when (event) {
            is TestQuestionEvent.Load -> load(event.testId)
            TestQuestionEvent.Refresh -> refresh()
        }
    }

    fun setTest(testId: String, testTitle: String) {
        setState { copy(testId = testId, testTitle = testTitle) }
        load(testId)
    }

    private fun load(testId: String) {
        viewModelScope.launch {
            when (val result = firebaseDataSource.getTestQuestions(testId)) {
                is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                is Resource.Success -> setState { copy(isLoading = false, questions = result.data ?: emptyList(), error = null) }
                is Resource.Error -> {
                    setState { copy(isLoading = false, error = result.message) }
                    showNotification(result.message ?: "Tải câu hỏi thất bại", NotificationType.ERROR)
                }
            }
        }
    }

    private fun refresh() {
        val id = state.value.testId
        if (id.isNotEmpty()) load(id)
    }
}


