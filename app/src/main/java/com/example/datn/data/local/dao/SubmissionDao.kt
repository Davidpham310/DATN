package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.SubmissionEntity

@Dao
interface SubmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity)

    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId")
    suspend fun getByAssignment(assignmentId: String): List<SubmissionEntity>

    @Query("SELECT * FROM submissions WHERE studentId = :studentId")
    suspend fun getByStudent(studentId: String): List<SubmissionEntity>
}
