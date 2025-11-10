package com.example.datn.presentation.student.tests

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.models.TestOption

data class StudentTestTakingState(
    val test: Test? = null,
    val questions: List<QuestionWithOptions> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val answers: Map<String, Answer> = emptyMap(),
    val startTime: Long = 0L,
    val timeRemaining: Long = 0L,
    val lastSavedTime: Long = 0L,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val showSubmitDialog: Boolean = false,
    val showQuestionList: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState {
    val currentQuestion: QuestionWithOptions?
        get() = questions.getOrNull(currentQuestionIndex)
    
    val answeredCount: Int
        get() = answers.size
    
    val progress: Float
        get() = if (questions.isEmpty()) 0f
                else answeredCount.toFloat() / questions.size
    
    val canGoNext: Boolean
        get() = currentQuestionIndex < questions.size - 1
    
    val canGoPrevious: Boolean
        get() = currentQuestionIndex > 0
    
    val hasAnsweredCurrent: Boolean
        get() = currentQuestion?.let { answers.containsKey(it.question.id) } ?: false
    
    fun getFormattedTimeRemaining(): String {
        val hours = timeRemaining / 3600
        val minutes = (timeRemaining % 3600) / 60
        val seconds = timeRemaining % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

data class QuestionWithOptions(
    val question: TestQuestion,
    val options: List<TestOption>
)
