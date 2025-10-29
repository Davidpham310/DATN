package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.datn.core.base.BaseDao
import com.example.datn.data.local.entities.MiniGameOptionEntity

@Dao
interface MiniGameOptionDao : BaseDao<MiniGameOptionEntity> {

    @Query("SELECT * FROM minigame_option WHERE miniGameQuestionId = :questionId")
    suspend fun getOptionsByQuestion(questionId: String): List<MiniGameOptionEntity>

    @Query("SELECT * FROM minigame_option WHERE miniGameQuestionId = :questionId AND isCorrect = 1")
    suspend fun getCorrectOptionForQuestion(questionId: String): MiniGameOptionEntity?

    @Query("DELETE FROM minigame_option WHERE miniGameQuestionId = :questionId")
    suspend fun deleteOptionsByQuestion(questionId: String)

    @Query("SELECT * FROM minigame_option WHERE id = :optionId")
    suspend fun getOptionById(optionId: String): MiniGameOptionEntity?

    @Query("DELETE FROM minigame_option WHERE id = :optionId")
    suspend fun deleteById(optionId: String)
}