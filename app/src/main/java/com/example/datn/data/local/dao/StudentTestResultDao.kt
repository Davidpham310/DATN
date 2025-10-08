package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.StudentTestResultEntity

@Dao
interface StudentTestResultDao : BaseDao<StudentTestResultEntity> {
    @Query("SELECT * FROM student_test_result WHERE studentId = :studentId AND testId = :testId")
    suspend fun getResultByStudentAndTest(studentId: String, testId: String): StudentTestResultEntity?

    @Query("SELECT * FROM student_test_result WHERE testId = :testId")
    suspend fun getResultsByTest(testId: String): List<StudentTestResultEntity>
}