package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.ClassStudentEntity

@Dao
interface ClassStudentDao : BaseDao<ClassStudentEntity> {
    @Query("SELECT * FROM class_student WHERE classId = :classId")
    suspend fun getStudentsInClass(classId: String): List<ClassStudentEntity>
}