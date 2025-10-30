package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.repository.ITestQuestionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TestQuestionUseCases @Inject constructor(
    private val repository: ITestQuestionRepository
) {
    fun create(question: TestQuestion): Flow<Resource<TestQuestion>> = repository.createQuestion(question)
    fun update(question: TestQuestion): Flow<Resource<TestQuestion>> = repository.updateQuestion(question)
    fun delete(questionId: String): Flow<Resource<Unit>> = repository.deleteQuestion(questionId)
    fun getById(questionId: String): Flow<Resource<TestQuestion?>> = repository.getQuestionById(questionId)
    fun listByTest(testId: String): Flow<Resource<List<TestQuestion>>> = repository.getQuestionsByTest(testId)
}
