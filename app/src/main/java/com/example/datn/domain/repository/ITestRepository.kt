package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentTestAnswer
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.models.TestOption
import kotlinx.coroutines.flow.Flow

interface ITestRepository {
    // Test CRUD
    fun createTest(test: Test): Flow<Resource<Test>>
    fun updateTest(test: Test): Flow<Resource<Test>>
    fun deleteTest(testId: String): Flow<Resource<Unit>>
    fun getTestDetails(testId: String): Flow<Resource<Test>>
    fun getTestsByLesson(lessonId: String): Flow<Resource<List<Test>>>
    
    // NEW: Get tests by multiple classes
    fun getTestsByClasses(classIds: List<String>): Flow<Resource<List<Test>>>
    
    // Test submission
    fun submitTest(studentId: String, testId: String, answers: Map<String, List<String>>): Flow<Resource<StudentTestResult>>
    
    // NEW: Submit with full result object
    fun submitTestResult(result: StudentTestResult, answers: Map<String, Any>): Flow<Resource<StudentTestResult>>
    
    // Test results
    fun getStudentResult(studentId: String, testId: String): Flow<Resource<StudentTestResult?>>
    fun getResultsByTest(testId: String): Flow<Resource<List<StudentTestResult>>>
    
    // NEW: Get all results for a student
    fun getStudentTestResults(studentId: String): Flow<Resource<List<StudentTestResult>>>
    
    // NEW: Questions and options
    fun getTestQuestions(testId: String): Flow<Resource<List<TestQuestion>>>
    fun getQuestionOptions(questionId: String): Flow<Resource<List<TestOption>>>
    
    // NEW: Student answers
    fun getStudentAnswers(resultId: String): Flow<Resource<List<StudentTestAnswer>>>
    
    // NEW: Manual grading support
    fun updateStudentAnswer(answer: StudentTestAnswer): Flow<Resource<StudentTestAnswer>>
    fun updateTestResult(result: StudentTestResult): Flow<Resource<StudentTestResult>>
}