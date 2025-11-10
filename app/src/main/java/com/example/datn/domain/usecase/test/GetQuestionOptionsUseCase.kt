package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQuestionOptionsUseCase @Inject constructor(
    private val testRepository: ITestRepository
) {
    operator fun invoke(questionId: String): Flow<Resource<List<TestOption>>> {
        return testRepository.getQuestionOptions(questionId)
    }
}
