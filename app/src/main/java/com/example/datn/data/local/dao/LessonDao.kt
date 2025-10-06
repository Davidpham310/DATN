package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.LessonEntity

@Dao
interface LessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)

    @Query("SELECT * FROM lessons WHERE classId = :classId ORDER BY orderIndex ASC")
    suspend fun getLessonsForClass(classId: String): List<LessonEntity>

    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
}
