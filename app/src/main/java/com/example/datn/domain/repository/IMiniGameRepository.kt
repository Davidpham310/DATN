package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion
import kotlinx.coroutines.flow.Flow

interface IMiniGameRepository {
    fun createGame(game: MiniGame): Flow<Resource<MiniGame>>
    fun getGameById(gameId: String): Flow<Resource<MiniGame?>>
    fun getQuestionsByGame(gameId: String): Flow<Resource<List<MiniGameQuestion>>>
    fun getFilteredGames(type: String?, level: String?): Flow<Resource<List<MiniGame>>>
}