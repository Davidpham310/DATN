package com.example.datn.presentation.student.games

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.presentation.student.tests.Answer

data class MiniGameResultState(
    val miniGame: MiniGame? = null,
    val result: StudentMiniGameResult? = null,
    val questions: List<QuestionWithAnswer> = emptyList(),
    val allResults: List<StudentMiniGameResult> = emptyList(),  // All attempts for history
    val showDetailedAnswers: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState {
    val scorePercentage: Double
        get() = if (result?.maxScore != null && result.maxScore > 0)
                (result.score / result.maxScore) * 100
                else 0.0
    
    val gradeText: String
        get() = when {
            scorePercentage >= 90 -> "Xuất sắc ⭐⭐⭐"
            scorePercentage >= 80 -> "Giỏi ⭐⭐"
            scorePercentage >= 70 -> "Khá ⭐"
            scorePercentage >= 50 -> "Trung bình"
            else -> "Yếu"
        }
    
    val correctCount: Int
        get() = questions.count { it.isCorrect }
    
    val durationText: String
        get() {
            val seconds = result?.durationSeconds ?: 0
            return when {
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> {
                    val mins = seconds / 60
                    val secs = seconds % 60
                    if (secs > 0) "${mins}m ${secs}s" else "${mins}m"
                }
                else -> {
                    val hours = seconds / 3600
                    val mins = (seconds % 3600) / 60
                    if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
                }
            }
        }
    
    val bestScore: Double?
        get() = allResults.maxOfOrNull { it.score }
    
    val attemptCount: Int
        get() = allResults.size
}

data class QuestionWithAnswer(
    val question: MiniGameQuestion,
    val options: List<MiniGameOption>,
    val studentAnswer: Answer?,
    val correctAnswer: Answer,
    val earnedScore: Double,
    val isCorrect: Boolean
) {
    val correctOptionIds: Set<String>
        get() = options.filter { it.isCorrect }.map { it.id }.toSet()
    
    val studentSelectedIds: Set<String>
        get() = when (studentAnswer) {
            is Answer.SingleChoice -> setOf(studentAnswer.optionId)
            is Answer.MultipleChoice -> studentAnswer.optionIds
            else -> emptySet()
        }
}
