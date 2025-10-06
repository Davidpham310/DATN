package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.ResourceEntity

@Dao
interface ResourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resource: ResourceEntity)

    @Query("SELECT * FROM resources WHERE subject = :subject")
    suspend fun getBySubject(subject: String): List<ResourceEntity>
}
