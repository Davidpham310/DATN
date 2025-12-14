package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentTestAnswer
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.Test
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TestUseCases @Inject constructor(
    private val repository: ITestRepository
) {
    // Teacher/Admin test management
    fun createTest(test: Test): Flow<Resource<Test>> = repository.createTest(test)
    fun updateTest(test: Test): Flow<Resource<Test>> = repository.updateTest(test)
    fun deleteTest(testId: String): Flow<Resource<Unit>> = repository.deleteTest(testId)
    fun getDetails(testId: String): Flow<Resource<Test>> = repository.getTestDetails(testId)
    fun listByLesson(lessonId: String): Flow<Resource<List<Test>>> = repository.getTestsByLesson(lessonId)
    
    // Student test submission
    fun submit(studentId: String, testId: String, answers: Map<String, List<String>>): Flow<Resource<StudentTestResult>> =
        repository.submitTest(studentId, testId, answers)
    
    // Test results
    fun getStudentResult(studentId: String, testId: String): Flow<Resource<StudentTestResult?>> =
        repository.getStudentResult(studentId, testId)
    fun getResultsByTest(testId: String): Flow<Resource<List<StudentTestResult>>> =
        repository.getResultsByTest(testId)
    
    // ========== NEW: Student Test System ==========
    
    // Get tests by multiple class IDs (for student viewing all their tests)
    fun getTestsByClasses(classIds: List<String>): Flow<Resource<List<com.example.datn.domain.models.Test>>> =
        repository.getTestsByClasses(classIds)
    
    // Get all test results for a student
    fun getStudentTestResults(studentId: String): Flow<Resource<List<StudentTestResult>>> =
        repository.getStudentTestResults(studentId)
    
    // Get questions for a specific test
    fun getTestQuestions(testId: String): Flow<Resource<List<com.example.datn.domain.models.TestQuestion>>> =
        repository.getTestQuestions(testId)
    
    // Get options for a specific question
    fun getQuestionOptions(questionId: String): Flow<Resource<List<com.example.datn.domain.models.TestOption>>> =
        repository.getQuestionOptions(questionId)
    
    // Submit test result with detailed answers
    fun submitTestResult(
        result: StudentTestResult,
        answers: Map<String, Any>
    ): Flow<Resource<StudentTestResult>> =
        repository.submitTestResult(result, answers)
    
    // Get student answers for a result
    fun getStudentAnswers(resultId: String): Flow<Resource<List<StudentTestAnswer>>> =
        repository.getStudentAnswers(resultId)

    fun updateStudentAnswer(answer: StudentTestAnswer): Flow<Resource<StudentTestAnswer>> =
        repository.updateStudentAnswer(answer)

    fun updateTestResult(result: StudentTestResult): Flow<Resource<StudentTestResult>> =
        repository.updateTestResult(result)
}


