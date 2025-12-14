package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.QuestionType
import com.example.datn.domain.models.StudentTestAnswer
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.TestStatus
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject

class GradeEssayAnswersUseCase @Inject constructor(
    private val testRepository: ITestRepository
) {
    operator fun invoke(
        result: StudentTestResult,
        essayScoresByQuestionId: Map<String, Double>
    ): Flow<Resource<StudentTestResult>> = flow {
        emit(Resource.Loading())

        val questionsRes = awaitFinal(testRepository.getTestQuestions(result.testId))
        val questions = (questionsRes as? Resource.Success)?.data.orEmpty()
        val scoreByQuestion = questions.associateBy({ it.id }, { it.score })
        val essayQuestionIds = questions.filter { it.questionType == QuestionType.ESSAY }.map { it.id }.toSet()

        val answersRes = awaitFinal(testRepository.getStudentAnswers(result.id))
        val answers = (answersRes as? Resource.Success)?.data.orEmpty().toMutableList()

        for ((questionId, rawScore) in essayScoresByQuestionId) {
            if (questionId !in essayQuestionIds) continue

            val maxScore = scoreByQuestion[questionId] ?: continue
            val normalizedScore = rawScore.coerceIn(0.0, maxScore)

            val index = answers.indexOfFirst { it.questionId == questionId }
            if (index == -1) continue

            val existing = answers[index]
            val updatedAnswer = existing.copy(
                earnedScore = normalizedScore,
                isCorrect = normalizedScore >= maxScore,
                updatedAt = Instant.now()
            )

            val updateRes = awaitFinal(testRepository.updateStudentAnswer(updatedAnswer))
            val saved = (updateRes as? Resource.Success)?.data
            if (saved == null) {
                emit(Resource.Error("Không thể cập nhật điểm tự luận"))
                return@flow
            }

            answers[index] = saved
        }

        val newTotal = answers.sumOf { it.earnedScore }
        val updatedResult = result.copy(
            score = newTotal,
            completionStatus = TestStatus.GRADED,
            updatedAt = Instant.now()
        )

        val resultUpdateRes = awaitFinal(testRepository.updateTestResult(updatedResult))
        val savedResult = (resultUpdateRes as? Resource.Success)?.data
        if (savedResult == null) {
            emit(Resource.Error("Không thể cập nhật tổng điểm"))
            return@flow
        }

        emit(Resource.Success(savedResult))
    }

    private suspend fun <T> awaitFinal(flow: Flow<Resource<T>>): Resource<T> {
        var last: Resource<T> = Resource.Loading()
        flow.collect { value ->
            last = value
        }
        return last
    }
}
