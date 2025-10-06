package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.AssignmentEntity

@Dao
interface AssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity)

    @Query("SELECT * FROM assignments WHERE classId = :classId")
    suspend fun getAssignmentsForClass(classId: String): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE id = :id LIMIT 1")
    suspend fun getAssignmentById(id: String): AssignmentEntity?

    @Delete
    suspend fun deleteAssignment(assignment: AssignmentEntity)
}
