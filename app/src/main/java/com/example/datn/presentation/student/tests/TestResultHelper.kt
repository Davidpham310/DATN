package com.example.datn.presentation.student.tests

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.StudentTestAnswer
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.usecase.test.TestUseCases
import kotlinx.coroutines.flow.collect

/**
 * Helper để build QuestionWithAnswer list từ questions và answers
 */
suspend fun buildQuestionsWithAnswers(
    questions: List<TestQuestion>,
    studentAnswers: List<StudentTestAnswer>,
    testUseCases: TestUseCases
): List<QuestionWithAnswer> {
    val result = mutableListOf<QuestionWithAnswer>()
    
    questions.forEach { question ->
        // Load options cho câu hỏi này
        var options: List<TestOption> = emptyList()
        testUseCases.getQuestionOptions(question.id).collect { optionsResult ->
            if (optionsResult is Resource.Success) {
                options = optionsResult.data ?: emptyList()
            }
        }
        
        // Tìm câu trả lời của học sinh
        val studentAnswer = studentAnswers.find { it.questionId == question.id }
        
        // Parse answer
        val parsedAnswer = studentAnswer?.let { parseAnswer(it, question.questionType) }
        
        // Build correct answer
        val correctAnswer = buildCorrectAnswer(question, options)
        
        // ========== DEBUG LOGGING ==========
        android.util.Log.d("TestResultHelper", """
            Question ${question.order}: ${question.content}
            Type: ${question.questionType}
            Student Answer String: ${studentAnswer?.answer}
            Parsed Answer: $parsedAnswer
            Correct Answer: $correctAnswer
            isCorrect (from DB): ${studentAnswer?.isCorrect}
            EarnedScore: ${studentAnswer?.earnedScore}
        """.trimIndent())
        // ====================================
        
        // ✅ RE-CALCULATE isCorrect thay vì dùng từ DB
        val isCorrectCalculated = calculateIsCorrect(parsedAnswer, correctAnswer)
        val earnedScoreCalculated = if (isCorrectCalculated) question.score else 0.0
        
        // Create QuestionWithAnswer
        val questionWithAnswer = QuestionWithAnswer(
            question = question,
            options = options,
            studentAnswer = parsedAnswer,
            correctAnswer = correctAnswer,
            earnedScore = earnedScoreCalculated,  // Use calculated score
            isCorrect = isCorrectCalculated       // Use calculated isCorrect
        )
        
        result.add(questionWithAnswer)
    }
    
    return result.sortedBy { it.question.order }
}

/**
 * Parse answer string thành Answer object
 */
fun parseAnswer(answer: StudentTestAnswer, questionType: QuestionType): Answer {
    return when (questionType) {
        QuestionType.SINGLE_CHOICE -> Answer.SingleChoice(answer.answer)
        QuestionType.MULTIPLE_CHOICE -> {
            val ids = answer.answer.split(",").filter { it.isNotBlank() }.toSet()
            Answer.MultipleChoice(ids)
        }
        QuestionType.FILL_BLANK -> Answer.FillBlank(answer.answer)
        QuestionType.ESSAY -> Answer.Essay(answer.answer)
    }
}

/**
 * Calculate if student answer is correct by comparing with correct answer
 */
private fun calculateIsCorrect(
    studentAnswer: Answer?,
    correctAnswer: Answer
): Boolean {
    if (studentAnswer == null) return false
    
    return when {
        studentAnswer is Answer.SingleChoice && correctAnswer is Answer.SingleChoice -> {
            studentAnswer.optionId == correctAnswer.optionId
        }
        studentAnswer is Answer.MultipleChoice && correctAnswer is Answer.MultipleChoice -> {
            studentAnswer.optionIds == correctAnswer.optionIds
        }
        studentAnswer is Answer.FillBlank && correctAnswer is Answer.FillBlank -> {
            studentAnswer.text.trim().lowercase() == correctAnswer.text.trim().lowercase()
        }
        studentAnswer is Answer.Essay && correctAnswer is Answer.Essay -> {
            // Essay không thể tự động chấm, cần giáo viên
            false
        }
        else -> false
    }
}

/**
 * Build correct answer từ options
 */
fun buildCorrectAnswer(question: TestQuestion, options: List<TestOption>): Answer {
    return when (question.questionType) {
        QuestionType.SINGLE_CHOICE -> {
            val correctOption = options.find { it.isCorrect }
            Answer.SingleChoice(correctOption?.id ?: "")
        }
        QuestionType.MULTIPLE_CHOICE -> {
            val correctIds = options.filter { it.isCorrect }.map { it.id }.toSet()
            Answer.MultipleChoice(correctIds)
        }
        QuestionType.FILL_BLANK -> {
            val correctOption = options.find { it.isCorrect }
            Answer.FillBlank(correctOption?.content ?: "")
        }
        QuestionType.ESSAY -> Answer.Essay("") // No predefined correct answer for essay
    }
}
