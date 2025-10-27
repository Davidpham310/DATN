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
    fun getGamesByTeacher(teacherId: String): Flow<Resource<List<MiniGame>>>
    fun getGamesByLesson(lessonId: String): Flow<Resource<List<MiniGame>>>
    fun updateGame(game: MiniGame): Flow<Resource<MiniGame>>
    fun deleteGame(gameId: String): Flow<Resource<Unit>>
    fun searchGames(query: String, teacherId: String? = null): Flow<Resource<List<MiniGame>>>
    
    // Question methods
    fun createQuestion(question: MiniGameQuestion): Flow<Resource<MiniGameQuestion>>
    fun updateQuestion(question: MiniGameQuestion): Flow<Resource<MiniGameQuestion>>
    fun deleteQuestion(questionId: String): Flow<Resource<Unit>>
    fun getQuestionById(questionId: String): Flow<Resource<MiniGameQuestion?>>
}