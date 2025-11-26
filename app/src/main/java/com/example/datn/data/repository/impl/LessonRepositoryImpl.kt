package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.core.utils.firebase.FirebaseErrorMapper
import com.example.datn.data.local.dao.LessonDao
import com.example.datn.data.local.dao.LessonContentDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import com.example.datn.domain.repository.ILessonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LessonRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val lessonDao: LessonDao,
) : ILessonRepository {

    override fun createLesson(lesson: Lesson): Flow<Resource<Lesson>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.addLesson(lesson)
            when (result) {
                is Resource.Success -> {
                    result.data?.let { createdLesson ->
                        lessonDao.insert(createdLesson.toEntity())
                        emit(Resource.Success(createdLesson))
                    } ?: emit(Resource.Error("Failed to create lesson"))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    override fun getLessonsByClass(classId: String): Flow<Resource<List<Lesson>>> = flow {
        emit(Resource.Loading())
        try {
            // Try to load from local first
            val localLessons = lessonDao.getLessonsByClass(classId).map { it.toDomain() }
            if (localLessons.isNotEmpty()) {
                emit(Resource.Success(localLessons))
            }

            // Fetch from remote
            val result = firebaseDataSource.getLessonsByClass(classId)
            when (result) {
                is Resource.Success -> {
                    result.data?.let { lessons ->
                        // Cache to local
                        lessons.forEach { lessonDao.insert(it.toEntity()) }
                        emit(Resource.Success(lessons))
                    }
                }
                is Resource.Error -> {
                    if (localLessons.isEmpty()) {
                        emit(Resource.Error(result.message))
                    }
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    fun updateLesson(lessonId: String, lesson: Lesson): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.updateLesson(lessonId, lesson)
            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        lessonDao.update(lesson.toEntity())
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to update lesson"))
                    }
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    fun deleteLesson(lessonId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.deleteLesson(lessonId)
            when (result) {
                is Resource.Success -> {
                    if (result.data) {
                        // Delete from local database
                        val lessonEntity = lessonDao.getLessonById(lessonId)
                        lessonEntity?.let { lessonDao.delete(it) }
                        emit(Resource.Success(true))
                    } else {
                        emit(Resource.Error("Failed to delete lesson"))
                    }
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }

    fun getLessonById(lessonId: String): Flow<Resource<Lesson?>> = flow {
        emit(Resource.Loading())
        try {
            val result = firebaseDataSource.getLessonById(lessonId)
            when (result) {
                is Resource.Success -> {
                    result.data?.let { lesson ->
                        lessonDao.insert(lesson.toEntity())
                        emit(Resource.Success(lesson))
                    } ?: emit(Resource.Success(null))
                }
                is Resource.Error -> {
                    val localLesson = lessonDao.getLessonById(lessonId)?.toDomain()
                    if (localLesson != null) {
                        emit(Resource.Success(localLesson))
                    } else {
                        emit(Resource.Error(result.message))
                    }
                }
                is Resource.Loading -> { /* Skip */ }
            }
        } catch (e: Exception) {
            emit(Resource.Error(FirebaseErrorMapper.getErrorMessage(e)))
        }
    }
}