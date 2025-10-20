package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.LessonContentEntity

@Dao
interface LessonContentDao : BaseDao<LessonContentEntity> {
    @Query("SELECT * FROM lesson_content WHERE lessonId = :lessonId ORDER BY 'order' ")
    suspend fun getContentByLesson(lessonId: String): List<LessonContentEntity>
    @Query("SELECT * FROM lesson_content WHERE id = :contentId LIMIT 1")
    suspend fun getContentById(contentId: String): LessonContentEntity?

    @Query("SELECT * FROM lesson_content WHERE lessonId = :lessonId ORDER BY 'order' ")
    suspend fun getContentsByLessonId(lessonId: String): List<LessonContentEntity>

    @Query("DELETE FROM lesson_content WHERE id = :contentId")
    suspend fun deleteById(contentId: String): Int
}