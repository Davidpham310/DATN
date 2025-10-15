package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.ClassEntity
import com.example.datn.data.local.entities.ClassStudentEntity

@Dao
interface ClassDao {

    // ----------------- Class CRUD -----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classObj: ClassEntity)

    @Update
    suspend fun updateClass(classObj: ClassEntity)

    @Delete
    suspend fun deleteClass(classObj: ClassEntity)

    @Query("SELECT * FROM class WHERE id = :classId")
    suspend fun getClassById(classId: String): ClassEntity?

    @Query("SELECT * FROM class WHERE teacherId = :teacherId")
    suspend fun getClassesByTeacher(teacherId: String): List<ClassEntity>

    @Query("SELECT * FROM class WHERE id IN (SELECT classId FROM class_student WHERE studentId = :studentId)")
    suspend fun getClassesByStudent(studentId: String): List<ClassEntity>

    @Query("SELECT * FROM class")
    suspend fun getAllClasses(): List<ClassEntity>

    // ----------------- ClassStudent (Relation) -----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addStudentToClass(classStudent: ClassStudentEntity)

    @Update
    suspend fun updateClassStudent(classStudent: ClassStudentEntity)

    @Delete
    suspend fun removeStudentFromClass(classStudent: ClassStudentEntity)

    @Query("SELECT * FROM class_student WHERE classId = :classId")
    suspend fun getStudentsInClass(classId: String): List<ClassStudentEntity>

    @Query("SELECT * FROM class_student WHERE studentId = :studentId")
    suspend fun getClassesOfStudent(studentId: String): List<ClassStudentEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM class_student WHERE classId = :classId AND studentId = :studentId)")
    suspend fun isStudentInClass(classId: String, studentId: String): Boolean

    @Query("DELETE FROM class_student WHERE classId = :classId")
    suspend fun deleteAllStudentsFromClass(classId: String)
}
