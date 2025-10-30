package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.Test
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TestUseCases @Inject constructor(
    private val repository: ITestRepository
) {
    fun createTest(test: Test): Flow<Resource<Test>> = repository.createTest(test)
    fun updateTest(test: Test): Flow<Resource<Test>> = repository.updateTest(test)
    fun deleteTest(testId: String): Flow<Resource<Unit>> = repository.deleteTest(testId)
    fun getDetails(testId: String): Flow<Resource<Test>> = repository.getTestDetails(testId)
    fun listByLesson(lessonId: String): Flow<Resource<List<Test>>> = repository.getTestsByLesson(lessonId)
    fun submit(studentId: String, testId: String, answers: Map<String, List<String>>): Flow<Resource<StudentTestResult>> =
        repository.submitTest(studentId, testId, answers)
    fun getStudentResult(studentId: String, testId: String): Flow<Resource<StudentTestResult?>> =
        repository.getStudentResult(studentId, testId)
    fun getResultsByTest(testId: String): Flow<Resource<List<StudentTestResult>>> =
        repository.getResultsByTest(testId)
}


