package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.MiniGameQuestionEntity

@Dao
interface MiniGameQuestionDao : BaseDao<MiniGameQuestionEntity> {

    @Query("SELECT * FROM minigame_question WHERE miniGameId = :miniGameId ORDER BY 'order'")
    suspend fun getQuestionsByMiniGame(miniGameId: String): List<MiniGameQuestionEntity>

    @Query("SELECT * FROM minigame_question WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): MiniGameQuestionEntity?
}