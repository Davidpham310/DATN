package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.ClassEntity

@Dao
interface ClassDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity)

    @Query("SELECT * FROM classes WHERE teacherId = :teacherId")
    suspend fun getClassesByTeacher(teacherId: String): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    suspend fun getClassById(id: String): ClassEntity?

    @Delete
    suspend fun deleteClass(classEntity: ClassEntity)
}
