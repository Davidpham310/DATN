package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.ParentEntity

@Dao
interface ParentDao : BaseDao<ParentEntity> {
    @Query("SELECT * FROM parent WHERE id = :parentId")
    suspend fun getParentById(parentId: String): ParentEntity?

    @Query("SELECT * FROM student WHERE userId = :userId")
    suspend fun getParentByUserId(userId: String): ParentEntity?
}