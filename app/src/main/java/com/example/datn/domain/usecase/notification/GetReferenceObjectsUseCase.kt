package com.example.datn.domain.usecase.notification

import android.util.Log
import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IClassRepository
import com.example.datn.domain.repository.ILessonRepository
import com.example.datn.domain.repository.ILessonContentRepository
import com.example.datn.domain.repository.ITestRepository
import com.example.datn.domain.repository.IMiniGameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case để lấy danh sách các đối tượng tham chiếu theo type
 * 
 * Chức năng:
 * - Lấy danh sách classes, lessons, tests, mini games
 * - Format thành ReferenceObject để hiển thị trong dropdown
 */
class GetReferenceObjectsUseCase @Inject constructor(
    private val classRepository: IClassRepository,
    private val lessonRepository: ILessonRepository,
    private val lessonContentRepository: ILessonContentRepository,
    private val testRepository: ITestRepository,
    private val miniGameRepository: IMiniGameRepository
) {
    private val TAG = "GetReferenceObjectsUseCase"
    
    /**
     * Lấy danh sách reference objects theo type
     * 
     * @param type Loại đối tượng cần lấy
     * @param parentId ID của parent (ví dụ: classId cho lessons)
     * @return Flow<Resource<List<ReferenceObject>>>
     */
    operator fun invoke(
        type: ReferenceObjectType,
        parentId: String? = null
    ): Flow<Resource<List<ReferenceObject>>> = flow {
        emit(Resource.Loading())
        
        try {
            Log.d(TAG, "Fetching reference objects for type: ${type.displayName}")
            
            val objects = when (type) {
                ReferenceObjectType.NONE -> {
                    emptyList()
                }
                
                ReferenceObjectType.CLASS -> {
                    // Lấy tất cả classes
                    val result = classRepository.getAllClasses().first()
                    if (result is Resource.Success) {
                        result.data?.map { classItem ->
                            ReferenceObject(
                                id = classItem.id,
                                name = classItem.name,
                                type = ReferenceObjectType.CLASS
                            )
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
                
                ReferenceObjectType.LESSON -> {
                    // Lấy lessons của một class
                    if (parentId.isNullOrBlank()) {
                        Log.w(TAG, "parentId (classId) is required for LESSON type")
                        emptyList()
                    } else {
                        val result = lessonRepository.getLessonsByClass(parentId).first()
                        if (result is Resource.Success) {
                            result.data?.map { lesson ->
                                ReferenceObject(
                                    id = lesson.id,
                                    name = lesson.title,
                                    type = ReferenceObjectType.LESSON
                                )
                            } ?: emptyList()
                        } else {
                            emptyList()
                        }
                    }
                }
                
                ReferenceObjectType.LESSON_CONTENT -> {
                    // Lấy lesson contents của một lesson
                    if (parentId.isNullOrBlank()) {
                        Log.w(TAG, "parentId (lessonId) is required for LESSON_CONTENT type")
                        emptyList()
                    } else {
                        val result = lessonContentRepository.getContentByLesson(parentId).first()
                        if (result is Resource.Success) {
                            result.data?.map { content ->
                                ReferenceObject(
                                    id = content.id,
                                    name = content.title,
                                    type = ReferenceObjectType.LESSON_CONTENT
                                )
                            } ?: emptyList()
                        } else {
                            emptyList()
                        }
                    }
                }
                
                ReferenceObjectType.TEST -> {
                    // Lấy tests của một lesson
                    if (parentId.isNullOrBlank()) {
                        Log.w(TAG, "parentId (lessonId) is required for TEST type")
                        emptyList()
                    } else {
                        val result = testRepository.getTestsByLesson(parentId).first()
                        if (result is Resource.Success) {
                            result.data?.map { test ->
                                ReferenceObject(
                                    id = test.id,
                                    name = test.title,
                                    type = ReferenceObjectType.TEST
                                )
                            } ?: emptyList()
                        } else {
                            emptyList()
                        }
                    }
                }
                
                ReferenceObjectType.MINI_GAME -> {
                    // Lấy mini games của một lesson
                    if (parentId.isNullOrBlank()) {
                        Log.w(TAG, "parentId (lessonId) is required for MINI_GAME type")
                        emptyList()
                    } else {
                        val result = miniGameRepository.getGamesByLesson(parentId).first()
                        if (result is Resource.Success) {
                            result.data?.map { game ->
                                ReferenceObject(
                                    id = game.id,
                                    name = game.title,
                                    type = ReferenceObjectType.MINI_GAME
                                )
                            } ?: emptyList()
                        } else {
                            emptyList()
                        }
                    }
                }
                
                ReferenceObjectType.MESSAGE -> {
                    // Messages không cần reference picker, skip
                    emptyList()
                }
            }
            
            Log.d(TAG, "Found ${objects.size} objects for type: ${type.displayName}")
            emit(Resource.Success(objects))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching reference objects", e)
            emit(Resource.Error("Không thể tải danh sách: ${e.message}"))
        }
    }
}
