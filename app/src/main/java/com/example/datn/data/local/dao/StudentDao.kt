package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.StudentEntity


@Dao
interface StudentDao : BaseDao<StudentEntity> {
    @Query("SELECT * FROM student WHERE id = :studentId")
    suspend fun getStudentById(studentId: String): StudentEntity?

    @Query("SELECT * FROM student WHERE userId = :userId")
    suspend fun getStudentByUserId(userId: String): StudentEntity?
}