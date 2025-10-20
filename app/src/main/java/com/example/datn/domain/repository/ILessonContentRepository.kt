package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.LessonContent
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface ILessonContentRepository {
    fun getContentByLesson(lessonId: String): Flow<Resource<List<LessonContent>>>

    fun getContentById(contentId: String): Flow<Resource<LessonContent>>

    fun addContent(
        content: LessonContent,
        fileStream: InputStream? = null,
        fileSize: Long = 0
    ): Flow<Resource<LessonContent>>

    fun updateContent(
        contentId: String,
        content: LessonContent,
        newFileStream: InputStream? = null,
        newFileSize: Long = 0
    ): Flow<Resource<Boolean>>

    fun deleteContent(contentId: String): Flow<Resource<Boolean>>
}
