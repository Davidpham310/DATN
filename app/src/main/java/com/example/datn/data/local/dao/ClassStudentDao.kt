package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.ClassStudentEntity

@Dao
interface ClassStudentDao : BaseDao<ClassStudentEntity> {
    @Query("SELECT * FROM class_student WHERE classId = :classId")
    suspend fun getStudentsInClass(classId: String): List<ClassStudentEntity>
    
    // Alias for messaging compatibility
    @Query("SELECT * FROM class_student WHERE classId = :classId")
    suspend fun getStudentsByClassId(classId: String): List<ClassStudentEntity>
    
    @Query("SELECT * FROM class_student WHERE studentId = :studentId")
    suspend fun getClassesByStudentId(studentId: String): List<ClassStudentEntity>
}