package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.MiniGameEntity
import com.example.datn.domain.models.GameType
import com.example.datn.domain.models.Level

@Dao
interface MiniGameDao : BaseDao<MiniGameEntity> {

    @Query("SELECT * FROM minigame WHERE id = :gameId")
    suspend fun getMiniGameById(gameId: String): MiniGameEntity?

    @Query("SELECT * FROM minigame WHERE teacherId = :teacherId ORDER BY createdAt DESC")
    suspend fun getMiniGamesByTeacher(teacherId: String): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE gameType = :type AND level = :level")
    suspend fun getMiniGamesByFilter(type: GameType, level: Level): List<MiniGameEntity>
}