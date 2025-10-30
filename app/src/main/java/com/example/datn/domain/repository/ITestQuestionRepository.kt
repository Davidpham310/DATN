package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestQuestion
import kotlinx.coroutines.flow.Flow

interface ITestQuestionRepository {
    fun createQuestion(question: TestQuestion): Flow<Resource<TestQuestion>>
    fun updateQuestion(question: TestQuestion): Flow<Resource<TestQuestion>>
    fun deleteQuestion(questionId: String): Flow<Resource<Unit>>
    fun getQuestionById(questionId: String): Flow<Resource<TestQuestion?>>
    fun getQuestionsByTest(testId: String): Flow<Resource<List<TestQuestion>>>
}
