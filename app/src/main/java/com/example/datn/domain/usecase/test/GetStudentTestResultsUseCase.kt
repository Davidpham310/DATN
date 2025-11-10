package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStudentTestResultsUseCase @Inject constructor(
    private val testRepository: ITestRepository
) {
    operator fun invoke(studentId: String): Flow<Resource<List<StudentTestResult>>> {
        return testRepository.getStudentTestResults(studentId)
    }
}
