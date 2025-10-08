package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.LessonContentEntity

@Dao
interface LessonContentDao : BaseDao<LessonContentEntity> {
    @Query("SELECT * FROM lesson_content WHERE lessonId = :lessonId ORDER BY 'order' ")
    suspend fun getContentByLesson(lessonId: String): List<LessonContentEntity>
}