package com.example.datn.presentation.teacher.test.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.StudentTestAnswer
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.usecase.test.GradeEssayAnswersUseCase
import com.example.datn.domain.usecase.test.TestUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherGradeEssayViewModel @Inject constructor(
    private val testUseCases: TestUseCases,
    private val gradeEssayAnswersUseCase: GradeEssayAnswersUseCase,
    notificationManager: NotificationManager
) : BaseViewModel<TeacherGradeEssayState, TeacherGradeEssayEvent>(
    TeacherGradeEssayState(),
    notificationManager
) {

    override fun onEvent(event: TeacherGradeEssayEvent) {
        when (event) {
            is TeacherGradeEssayEvent.Load -> load(event.testId, event.resultId)
            is TeacherGradeEssayEvent.ChangeScore -> changeScore(event.questionId, event.value)
            TeacherGradeEssayEvent.Submit -> submit()
        }
    }

    private fun load(testId: String, resultId: String) {
        viewModelScope.launch {
            setState { copy(isLoading = true, testId = testId, resultId = resultId, error = null) }

            val testRes = awaitFinal(testUseCases.getDetails(testId))
            val test = (testRes as? Resource.Success)?.data

            val questionsRes = awaitFinal(testUseCases.getTestQuestions(testId))
            val questions = (questionsRes as? Resource.Success)?.data.orEmpty()

            val answersRes = awaitFinal(testUseCases.getStudentAnswers(resultId))
            val answers = (answersRes as? Resource.Success)?.data.orEmpty()

            val essayItems = questions
                .filter { it.questionType == QuestionType.ESSAY }
                .sortedBy { it.order }
                .mapNotNull { q ->
                    val a = answers.find { it.questionId == q.id } ?: return@mapNotNull null
                    EssayAnswerUi(
                        question = q,
                        answer = a
                    )
                }

            val editable = essayItems.associate { it.question.id to it.answer.earnedScore.toString() }

            setState {
                copy(
                    isLoading = false,
                    test = test,
                    essayAnswers = essayItems,
                    scoreInputs = editable
                )
            }
        }
    }

    private fun changeScore(questionId: String, value: String) {
        setState { copy(scoreInputs = scoreInputs + (questionId to value)) }
    }

    private fun submit() {
        val current = state.value
        val resultId = current.resultId
        val testId = current.testId

        if (resultId.isBlank() || testId.isBlank()) return

        viewModelScope.launch {
            setState { copy(isSubmitting = true) }

            val answersRes = awaitFinal(testUseCases.getStudentAnswers(resultId))
            val answers = (answersRes as? Resource.Success)?.data
            if (answers == null) {
                setState { copy(isSubmitting = false) }
                showNotification("Không thể tải câu trả lời để chấm", NotificationType.ERROR)
                return@launch
            }

            val resultRes = awaitFinal(testUseCases.getResultsByTest(testId))
            val result = (resultRes as? Resource.Success)?.data?.find { it.id == resultId }
            if (result == null) {
                setState { copy(isSubmitting = false) }
                showNotification("Không tìm thấy kết quả cần chấm", NotificationType.ERROR)
                return@launch
            }

            val scores = current.scoreInputs.mapNotNull { (qId, raw) ->
                val d = raw.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                qId to d
            }.toMap()

            gradeEssayAnswersUseCase(result, scores)
                .collect { res ->
                    when (res) {
                        is Resource.Loading -> setState { copy(isSubmitting = true) }
                        is Resource.Success -> {
                            setState { copy(isSubmitting = false) }
                            showNotification("Đã chấm bài thành công", NotificationType.SUCCESS)
                        }
                        is Resource.Error -> {
                            setState { copy(isSubmitting = false) }
                            showNotification(res.message ?: "Chấm bài thất bại", NotificationType.ERROR)
                        }
                    }
                }
        }
    }

    private suspend fun <T> awaitFinal(flow: Flow<Resource<T>>): Resource<T> {
        var last: Resource<T> = Resource.Loading()
        flow.collect { value ->
            last = value
        }
        return last
    }
}

data class TeacherGradeEssayState(
    val testId: String = "",
    val resultId: String = "",
    val test: Test? = null,
    val essayAnswers: List<EssayAnswerUi> = emptyList(),
    val scoreInputs: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : com.example.datn.core.base.BaseState

data class EssayAnswerUi(
    val question: TestQuestion,
    val answer: StudentTestAnswer
)

sealed class TeacherGradeEssayEvent : com.example.datn.core.base.BaseEvent {
    data class Load(val testId: String, val resultId: String) : TeacherGradeEssayEvent()
    data class ChangeScore(val questionId: String, val value: String) : TeacherGradeEssayEvent()
    data object Submit : TeacherGradeEssayEvent()
}
