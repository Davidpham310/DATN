package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.LessonEntity

@Dao
interface LessonDao : BaseDao<LessonEntity> {
    @Query("SELECT * FROM lesson WHERE classId = :classId ORDER BY 'order'")
    suspend fun getLessonsByClass(classId: String): List<LessonEntity>
}