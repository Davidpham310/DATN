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

    @Query("SELECT * FROM minigame WHERE lessonId = :lessonId ORDER BY createdAt DESC")
    suspend fun getMiniGamesByLesson(lessonId: String): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE teacherId = :teacherId AND lessonId = :lessonId ORDER BY createdAt DESC")
    suspend fun getMiniGamesByTeacherAndLesson(teacherId: String, lessonId: String): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE gameType = :type AND level = :level")
    suspend fun getMiniGamesByFilter(type: GameType, level: Level): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE gameType = :type ORDER BY createdAt DESC")
    suspend fun getMiniGamesByType(type: GameType): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE level = :level ORDER BY createdAt DESC")
    suspend fun getMiniGamesByLevel(level: Level): List<MiniGameEntity>

    @Query("SELECT * FROM minigame ORDER BY createdAt DESC")
    suspend fun getAllMiniGames(): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') AND (:teacherId IS NULL OR teacherId = :teacherId) ORDER BY createdAt DESC")
    suspend fun searchMiniGames(query: String, teacherId: String? = null): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE gameType = :type AND teacherId = :teacherId ORDER BY createdAt DESC")
    suspend fun getMiniGamesByTypeAndTeacher(type: GameType, teacherId: String): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE level = :level AND teacherId = :teacherId ORDER BY createdAt DESC")
    suspend fun getMiniGamesByLevelAndTeacher(level: Level, teacherId: String): List<MiniGameEntity>

    @Query("SELECT * FROM minigame WHERE gameType = :type AND level = :level AND teacherId = :teacherId ORDER BY createdAt DESC")
    suspend fun getMiniGamesByFilterAndTeacher(type: GameType, level: Level, teacherId: String): List<MiniGameEntity>

    @Query("DELETE FROM minigame WHERE id = :gameId")
    suspend fun deleteById(gameId: String)
}