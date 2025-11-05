package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.ParentStudentEntity

@Dao
interface ParentStudentDao : BaseDao<ParentStudentEntity> {
    @Query("SELECT * FROM parent_student WHERE studentId = :studentId")
    suspend fun getParentsOfStudent(studentId: String): List<ParentStudentEntity>
    
    // Alias for messaging compatibility
    @Query("SELECT * FROM parent_student WHERE studentId = :studentId")
    suspend fun getParentsByStudentId(studentId: String): List<ParentStudentEntity>

    @Query("SELECT * FROM parent_student WHERE parentId = :parentId")
    suspend fun getStudentsOfParent(parentId: String): List<ParentStudentEntity>
    
    // Alias for messaging compatibility
    @Query("SELECT * FROM parent_student WHERE parentId = :parentId")
    suspend fun getStudentsByParentId(parentId: String): List<ParentStudentEntity>
}