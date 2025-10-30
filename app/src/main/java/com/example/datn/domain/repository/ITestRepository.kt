package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.Test
import kotlinx.coroutines.flow.Flow

interface ITestRepository {
    fun createTest(test: Test): Flow<Resource<Test>>
    fun updateTest(test: Test): Flow<Resource<Test>>
    fun deleteTest(testId: String): Flow<Resource<Unit>>
    fun getTestDetails(testId: String): Flow<Resource<Test>>
    fun getTestsByLesson(lessonId: String): Flow<Resource<List<Test>>>
    fun submitTest(studentId: String, testId: String, answers: Map<String, List<String>>): Flow<Resource<StudentTestResult>>
    fun getStudentResult(studentId: String, testId: String): Flow<Resource<StudentTestResult?>>
    fun getResultsByTest(testId: String): Flow<Resource<List<StudentTestResult>>>
}