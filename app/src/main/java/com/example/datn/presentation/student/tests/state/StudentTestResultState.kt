package com.example.datn.presentation.student.tests.state

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.models.TestOption

data class StudentTestResultState(
    val test: Test? = null,
    val result: StudentTestResult? = null,
    val questions: List<QuestionWithAnswer> = emptyList(),
    val classAverage: Double? = null,
    val classRank: Int? = null,
    val totalStudents: Int? = null,
    val showDetailedAnswers: Boolean = false,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseState {
    val scorePercentage: Double
        get() = if (test?.totalScore != null && test.totalScore > 0)
                (result?.score ?: 0.0) / test.totalScore * 100
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
    
    val durationMinutes: Long
        get() = (result?.durationSeconds ?: 0) / 60
    
    val durationSeconds: Long
        get() = (result?.durationSeconds ?: 0) % 60
    
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
    
    fun getRankText(): String? {
        return if (classRank != null && totalStudents != null) {
            "$classRank/$totalStudents"
        } else null
    }
}

data class QuestionWithAnswer(
    val question: TestQuestion,
    val options: List<TestOption>,
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
