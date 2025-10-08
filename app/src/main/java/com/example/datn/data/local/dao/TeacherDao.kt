package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.TeacherEntity

@Dao
interface TeacherDao : BaseDao<TeacherEntity> {
    @Query("SELECT * FROM teacher WHERE id = :teacherId")
    suspend fun getTeacherById(teacherId: String): TeacherEntity?

    @Query("SELECT * FROM teacher WHERE userId = :userId")
    suspend fun getTeacherByUserId(userId: String): TeacherEntity?
}