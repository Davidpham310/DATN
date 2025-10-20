package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Lesson
import com.example.datn.domain.models.LessonContent
import kotlinx.coroutines.flow.Flow

interface ILessonRepository {
    fun createLesson(lesson: Lesson): Flow<Resource<Lesson>>
    fun getLessonsByClass(classId: String): Flow<Resource<List<Lesson>>>

}