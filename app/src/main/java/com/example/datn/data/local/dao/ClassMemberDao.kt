package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.ClassMemberEntity

@Dao
interface ClassMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: ClassMemberEntity)

    @Query("SELECT * FROM class_members WHERE classId = :classId")
    suspend fun getMembersByClass(classId: String): List<ClassMemberEntity>

    @Query("SELECT * FROM class_members WHERE studentId = :studentId")
    suspend fun getClassesForStudent(studentId: String): List<ClassMemberEntity>

    @Query("DELETE FROM class_members WHERE id = :id")
    suspend fun removeMember(id: String)
}
