package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.StudentTestResultDao
import com.example.datn.data.local.dao.TestDao
import com.example.datn.data.local.dao.TestQuestionDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.StudentTestResult
import com.example.datn.domain.models.Test
import com.example.datn.domain.models.TestStatus
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val testDao: TestDao,
    private val testQuestionDao: TestQuestionDao,
    private val studentTestResultDao: StudentTestResultDao
) : ITestRepository {

    override fun createTest(test: Test): Flow<Resource<Test>> = flow {
        try {
            emit(Resource.Loading())
            val withId = if (test.id.isBlank()) test.copy(id = UUID.randomUUID().toString()) else test
            val withTimestamps = withId.copy(
                createdAt = if (withId.createdAt.toEpochMilli() == 0L) Instant.now() else withId.createdAt,
                updatedAt = Instant.now()
            )
            when (val result = firebaseDataSource.addTest(withTimestamps)) {
                is Resource.Success -> {
                    val saved = result.data ?: withTimestamps
                    testDao.insert(saved.toEntity())
                    emit(Resource.Success(saved))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tạo bài kiểm tra: ${e.message}"))
        }
    }

    override fun updateTest(test: Test): Flow<Resource<Test>> = flow {
        try {
            emit(Resource.Loading())
            val withTimestamp = test.copy(updatedAt = Instant.now())
            when (val result = firebaseDataSource.updateTest(withTimestamp)) {
                is Resource.Success -> {
                    val updated = result.data ?: withTimestamp
                    testDao.insert(updated.toEntity())
                    emit(Resource.Success(updated))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi cập nhật bài kiểm tra: ${e.message}"))
        }
    }

    override fun deleteTest(testId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.deleteTest(testId)) {
                is Resource.Success -> {
                    testDao.deleteById(testId)
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi xóa bài kiểm tra: ${e.message}"))
        }
    }

    override fun getTestDetails(testId: String): Flow<Resource<Test>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getTestById(testId)) {
                is Resource.Success -> {
                    val remote = result.data
                    if (remote != null) {
                        testDao.insert(remote.toEntity())
                        emit(Resource.Success(remote))
                    } else {
                        val local = testDao.getById(testId)?.toDomain()
                        if (local != null) emit(Resource.Success(local)) else emit(Resource.Error("Không tìm thấy bài kiểm tra"))
                    }
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy thông tin bài kiểm tra: ${e.message}"))
        }
    }

    override fun getTestsByLesson(lessonId: String): Flow<Resource<List<Test>>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getTestsByLesson(lessonId)) {
                is Resource.Success -> {
                    val tests = result.data ?: emptyList()
                    tests.forEach { testDao.insert(it.toEntity()) }
                    emit(Resource.Success(tests))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách bài kiểm tra: ${e.message}"))
        }
    }

    override fun submitTest(
        studentId: String,
        testId: String,
        answers: Map<String, List<String>>
    ): Flow<Resource<StudentTestResult>> = flow {
        try {
            emit(Resource.Loading())
            // Scoring is domain-specific; here we just persist a result shell. Customize as needed.
            val now = Instant.now()
            val result = StudentTestResult(
                id = UUID.randomUUID().toString(),
                studentId = studentId,
                testId = testId,
                score = 0.0,
                completionStatus = TestStatus.COMPLETED,
                submissionTime = now,
                durationSeconds = 0,
                createdAt = now,
                updatedAt = now
            )
            when (val remote = firebaseDataSource.submitTestResult(result)) {
                is Resource.Success -> {
                    val saved = remote.data ?: result
                    studentTestResultDao.insert(saved.toEntity())
                    emit(Resource.Success(saved))
                }
                is Resource.Error -> emit(Resource.Error(remote.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi nộp bài kiểm tra: ${e.message}"))
        }
    }

    override fun getStudentResult(studentId: String, testId: String): Flow<Resource<StudentTestResult?>> = flow {
        try {
            emit(Resource.Loading())
            when (val remote = firebaseDataSource.getStudentResult(studentId, testId)) {
                is Resource.Success -> {
                    val res = remote.data
                    if (res != null) studentTestResultDao.insert(res.toEntity())
                    emit(Resource.Success(res))
                }
                is Resource.Error -> emit(Resource.Error(remote.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy kết quả: ${e.message}"))
        }
    }

    override fun getResultsByTest(testId: String): Flow<Resource<List<StudentTestResult>>> = flow {
        try {
            emit(Resource.Loading())
            when (val remote = firebaseDataSource.getResultsByTest(testId)) {
                is Resource.Success -> {
                    val list = remote.data ?: emptyList()
                    list.forEach { studentTestResultDao.insert(it.toEntity()) }
                    emit(Resource.Success(list))
                }
                is Resource.Error -> emit(Resource.Error(remote.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách kết quả: ${e.message}"))
        }
    }
}


