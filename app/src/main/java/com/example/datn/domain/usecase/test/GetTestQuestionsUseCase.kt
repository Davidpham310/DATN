package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTestQuestionsUseCase @Inject constructor(
    private val testRepository: ITestRepository
) {
    operator fun invoke(testId: String): Flow<Resource<List<TestQuestion>>> {
        return testRepository.getTestQuestions(testId)
    }
}
