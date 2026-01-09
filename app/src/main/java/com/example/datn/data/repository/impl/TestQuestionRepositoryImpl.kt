package com.example.datn.data.repository.impl

import com.example.datn.data.remote.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.TestQuestionDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.TestQuestion
import com.example.datn.domain.repository.ITestQuestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestQuestionRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val testQuestionDao: TestQuestionDao
) : ITestQuestionRepository {

    override fun createQuestion(question: TestQuestion): Flow<Resource<TestQuestion>> = flow {
        try {
            emit(Resource.Loading())
            val withId = if (question.id.isBlank()) question.copy(id = UUID.randomUUID().toString()) else question
            val withTimestamps = withId.copy(
                createdAt = if (withId.createdAt.toEpochMilli() == 0L) Instant.now() else withId.createdAt,
                updatedAt = Instant.now()
            )
            when (val result = firebaseDataSource.addTestQuestion(withTimestamps)) {
                is Resource.Success -> {
                    val saved = result.data ?: withTimestamps
                    testQuestionDao.insert(saved.toEntity())
                    emit(Resource.Success(saved))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tạo câu hỏi: ${e.message}"))
        }
    }

    override fun updateQuestion(question: TestQuestion): Flow<Resource<TestQuestion>> = flow {
        try {
            emit(Resource.Loading())
            val withTimestamp = question.copy(updatedAt = Instant.now())
            when (val result = firebaseDataSource.updateTestQuestion(withTimestamp)) {
                is Resource.Success -> {
                    val updated = result.data ?: withTimestamp
                    testQuestionDao.insert(updated.toEntity())
                    emit(Resource.Success(updated))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi cập nhật câu hỏi: ${e.message}"))
        }
    }

    override fun deleteQuestion(questionId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.deleteTestQuestion(questionId)) {
                is Resource.Success -> {
                    testQuestionDao.deleteById(questionId)
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi xóa câu hỏi: ${e.message}"))
        }
    }

    override fun getQuestionById(questionId: String): Flow<Resource<TestQuestion?>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getTestQuestionById(questionId)) {
                is Resource.Success -> {
                    val remote = result.data
                    if (remote != null) {
                        testQuestionDao.insert(remote.toEntity())
                        emit(Resource.Success(remote))
                    } else {
                        val local = testQuestionDao.getQuestionById(questionId)?.toDomain()
                        if (local != null) emit(Resource.Success(local)) else emit(Resource.Error("Không tìm thấy câu hỏi"))
                    }
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy thông tin câu hỏi: ${e.message}"))
        }
    }

    override fun getQuestionsByTest(testId: String): Flow<Resource<List<TestQuestion>>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getTestQuestions(testId)) {
                is Resource.Success -> {
                    val questions = result.data ?: emptyList()
                    questions.forEach { testQuestionDao.insert(it.toEntity()) }
                    emit(Resource.Success(questions))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách câu hỏi: ${e.message}"))
        }
    }
}
