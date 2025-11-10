package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SubmitTestResultUseCase @Inject constructor(
    private val testRepository: ITestRepository
) {
    operator fun invoke(
        result: StudentTestResult,
        answers: Map<String, Any>
    ): Flow<Resource<StudentTestResult>> {
        return testRepository.submitTestResult(result, answers)
    }
}
