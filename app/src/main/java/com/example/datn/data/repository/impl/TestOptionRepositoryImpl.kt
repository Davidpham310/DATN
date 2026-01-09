package com.example.datn.data.repository.impl

import com.example.datn.data.remote.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.TestOptionDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.repository.ITestOptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestOptionRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val testOptionDao: TestOptionDao
) : ITestOptionRepository {

    override fun createOption(option: TestOption): Flow<Resource<TestOption>> = flow {
        try {
            emit(Resource.Loading())
            val withId = if (option.id.isBlank()) option.copy(id = UUID.randomUUID().toString()) else option
            val withTimestamps = withId.copy(
                createdAt = if (withId.createdAt.toEpochMilli() == 0L) Instant.now() else withId.createdAt,
                updatedAt = Instant.now()
            )
            when (val result = firebaseDataSource.addTestOption(withTimestamps)) {
                is Resource.Success -> {
                    val saved = result.data ?: withTimestamps
                    testOptionDao.insert(saved.toEntity())
                    emit(Resource.Success(saved))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tạo đáp án: ${e.message}"))
        }
    }

    override fun updateOption(option: TestOption): Flow<Resource<TestOption>> = flow {
        try {
            emit(Resource.Loading())
            val updated = option.copy(updatedAt = Instant.now())
            when (val result = firebaseDataSource.updateTestOption(updated.id, updated)) {
                is Resource.Success -> {
                    testOptionDao.update(updated.toEntity())
                    emit(Resource.Success(updated))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi cập nhật đáp án: ${e.message}"))
        }
    }

    override fun deleteOption(optionId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.deleteTestOption(optionId)) {
                is Resource.Success -> {
                    testOptionDao.deleteById(optionId)
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi xóa đáp án: ${e.message}"))
        }
    }

    override fun getOptionsByQuestion(questionId: String): Flow<Resource<List<TestOption>>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getTestOptionsByQuestion(questionId)) {
                is Resource.Success -> {
                    val options = result.data ?: emptyList()
                    options.forEach { testOptionDao.insert(it.toEntity()) }
                    emit(Resource.Success(options))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tải đáp án: ${e.message}"))
        }
    }

    override fun getOptionById(optionId: String): Flow<Resource<TestOption?>> = flow {
        try {
            emit(Resource.Loading())
            val local = testOptionDao.getOptionById(optionId)?.toDomain()
            emit(Resource.Success(local))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy đáp án: ${e.message}"))
        }
    }
}


