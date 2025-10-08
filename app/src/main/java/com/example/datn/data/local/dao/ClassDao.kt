package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.ClassEntity

@Dao
interface ClassDao : BaseDao<ClassEntity> {
    @Query("SELECT * FROM class WHERE teacherId = :teacherId")
    suspend fun getClassesByTeacher(teacherId: String): List<ClassEntity>

    @Query("SELECT * FROM class WHERE id = :classId")
    suspend fun getClassById(classId: String): ClassEntity?
}