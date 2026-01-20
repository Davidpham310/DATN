package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.repository.ITestQuestionRepository
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class TestQuestionUseCases @Inject constructor(
    private val repository: ITestQuestionRepository,
    private val testRepository: ITestRepository
) {
    fun create(question: TestQuestion): Flow<Resource<TestQuestion>> = flow {
        emit(Resource.Loading())

        // Fetch Test details without using first()
        var testRes: Resource<Test>? = null
        testRepository.getTestDetails(question.testId).collect { res ->
            if (res !is Resource.Loading && testRes == null) {
                testRes = res
            }
        }

        val testResult = testRes
        if (testResult is Resource.Error) {
            emit(Resource.Error(testResult.message))
            return@flow
        }
        val test = (testResult as? Resource.Success)?.data
        if (test == null) {
            emit(Resource.Error("Không tìm thấy bài kiểm tra"))
            return@flow
        }

        // Fetch existing questions for the test without using first()
        var questionsRes: Resource<List<TestQuestion>>? = null
        repository.getQuestionsByTest(question.testId).collect { res ->
            if (res !is Resource.Loading && questionsRes == null) {
                questionsRes = res
            }
        }

        val qResult = questionsRes
        if (qResult is Resource.Error) {
            emit(Resource.Error(qResult.message))
            return@flow
        }

        val existing = (qResult as? Resource.Success)?.data?.sortedBy { it.order } ?: emptyList()
        val currentTotal = existing.sumOf { it.score }
        val newTotal = currentTotal + question.score

        if (newTotal > test.totalScore) {
            emit(Resource.Error("Tổng điểm câu hỏi (${newTotal}) vượt quá tổng điểm bài kiểm tra (${test.totalScore})."))
            return@flow
        }

        // Delegate create; order handling will be done atomically in TestService
        repository.createQuestion(question).collect { emit(it) }
    }

    fun update(question: TestQuestion): Flow<Resource<TestQuestion>> = flow {
        emit(Resource.Loading())

        // Fetch Test details without using first()
        var testRes: Resource<Test>? = null
        testRepository.getTestDetails(question.testId).collect { res ->
            if (res !is Resource.Loading && testRes == null) {
                testRes = res
            }
        }

        val testResult = testRes
        if (testResult is Resource.Error) {
            emit(Resource.Error(testResult.message))
            return@flow
        }
        val test = (testResult as? Resource.Success)?.data
        if (test == null) {
            emit(Resource.Error("Không tìm thấy bài kiểm tra"))
            return@flow
        }

        // Fetch existing questions and exclude the one being updated without using first()
        var questionsRes: Resource<List<TestQuestion>>? = null
        repository.getQuestionsByTest(question.testId).collect { res ->
            if (res !is Resource.Loading && questionsRes == null) {
                questionsRes = res
            }
        }

        val qResult = questionsRes
        if (qResult is Resource.Error) {
            emit(Resource.Error(qResult.message))
            return@flow
        }

        val allQuestions = (qResult as? Resource.Success)?.data?.sortedBy { it.order } ?: emptyList()
        val oldQuestion = allQuestions.find { it.id == question.id }
        if (oldQuestion == null) {
            emit(Resource.Error("Không tìm thấy câu hỏi"))
            return@flow
        }
        val others = allQuestions.filter { it.id != question.id }
        val otherTotal = others.sumOf { it.score }
        val newTotal = otherTotal + question.score

        if (newTotal > test.totalScore) {
            emit(Resource.Error("Tổng điểm câu hỏi (${newTotal}) vượt quá tổng điểm bài kiểm tra (${test.totalScore})."))
            return@flow
        }

        // Delegate update; order handling will be done atomically in TestService
        repository.updateQuestion(question).collect { emit(it) }
    }
    fun delete(questionId: String): Flow<Resource<Unit>> = repository.deleteQuestion(questionId)
    fun getById(questionId: String): Flow<Resource<TestQuestion?>> = repository.getQuestionById(questionId)
    fun listByTest(testId: String): Flow<Resource<List<TestQuestion>>> = repository.getQuestionsByTest(testId)
}
