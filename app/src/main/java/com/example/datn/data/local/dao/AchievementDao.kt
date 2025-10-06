package com.example.datn.data.local.dao

import androidx.room.*
import com.example.datn.data.local.entities.AchievementEntity

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(a: AchievementEntity)

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>
}
