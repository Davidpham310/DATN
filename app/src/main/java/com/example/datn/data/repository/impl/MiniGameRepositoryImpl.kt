package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.MiniGameDao
import com.example.datn.data.local.dao.MiniGameQuestionDao
import com.example.datn.data.local.dao.MiniGameOptionDao
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.GameType
import com.example.datn.domain.models.Level
import com.example.datn.domain.repository.IMiniGameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MiniGameRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val miniGameDao: MiniGameDao,
    private val miniGameQuestionDao: MiniGameQuestionDao,
    private val miniGameOptionDao: MiniGameOptionDao
) : IMiniGameRepository {

    override fun createGame(game: MiniGame): Flow<Resource<MiniGame>> = flow {
        try {
            emit(Resource.Loading())
            
            val gameWithId = if (game.id.isBlank()) {
                game.copy(
                    id = UUID.randomUUID().toString(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            } else {
                game.copy(updatedAt = Instant.now())
            }
            
            // Save to Firebase
            val result = firebaseDataSource.addMiniGame(gameWithId)
            when (result) {
                is Resource.Success -> {
                    val savedGame = result.data ?: gameWithId
                    // Also save to local database
                    miniGameDao.insert(savedGame.toEntity())
                    emit(Resource.Success(savedGame))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tạo mini game: ${e.message}"))
        }
    }

    override fun getGameById(gameId: String): Flow<Resource<MiniGame?>> = flow {
        try {
            emit(Resource.Loading())
            val game = miniGameDao.getMiniGameById(gameId)?.toDomain()
            emit(Resource.Success(game))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy mini game: ${e.message}"))
        }
    }

    override fun getQuestionsByGame(gameId: String): Flow<Resource<List<MiniGameQuestion>>> = flow {
        try {
            emit(Resource.Loading())
            val result = firebaseDataSource.getQuestionsByMiniGame(gameId)
            when (result) {
                is Resource.Success -> {
                    val questions = result.data ?: emptyList()
                    // Also save to local database
                    questions.forEach { question ->
                        miniGameQuestionDao.insert(question.toEntity())
                    }
                    emit(Resource.Success(questions))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách câu hỏi: ${e.message}"))
        }
    }

    override fun getFilteredGames(type: String?, level: String?): Flow<Resource<List<MiniGame>>> = flow {
        try {
            emit(Resource.Loading())
            
            val games = when {
                type != null && level != null -> {
                    val gameType = GameType.fromString(type)
                    val gameLevel = Level.fromString(level)
                    if (gameType != null && gameLevel != null) {
                        miniGameDao.getMiniGamesByFilter(gameType, gameLevel)
                    } else {
                        emptyList()
                    }
                }
                type != null -> {
                    val gameType = GameType.fromString(type)
                    if (gameType != null) {
                        miniGameDao.getMiniGamesByType(gameType)
                    } else {
                        emptyList()
                    }
                }
                level != null -> {
                    val gameLevel = Level.fromString(level)
                    if (gameLevel != null) {
                        miniGameDao.getMiniGamesByLevel(gameLevel)
                    } else {
                        emptyList()
                    }
                }
                else -> {
                    miniGameDao.getAllMiniGames()
                }
            }.map { it.toDomain() }
            
            emit(Resource.Success(games))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách mini game: ${e.message}"))
        }
    }

    fun getFilteredGamesByTeacher(type: String?, level: String?, teacherId: String): Flow<Resource<List<MiniGame>>> = flow {
        try {
            emit(Resource.Loading())
            
            val games = when {
                type != null && level != null -> {
                    val gameType = GameType.fromString(type)
                    val gameLevel = Level.fromString(level)
                    if (gameType != null && gameLevel != null) {
                        miniGameDao.getMiniGamesByFilterAndTeacher(gameType, gameLevel, teacherId)
                    } else {
                        emptyList()
                    }
                }
                type != null -> {
                    val gameType = GameType.fromString(type)
                    if (gameType != null) {
                        miniGameDao.getMiniGamesByTypeAndTeacher(gameType, teacherId)
                    } else {
                        emptyList()
                    }
                }
                level != null -> {
                    val gameLevel = Level.fromString(level)
                    if (gameLevel != null) {
                        miniGameDao.getMiniGamesByLevelAndTeacher(gameLevel, teacherId)
                    } else {
                        emptyList()
                    }
                }
                else -> {
                    miniGameDao.getMiniGamesByTeacher(teacherId)
                }
            }.map { it.toDomain() }
            
            emit(Resource.Success(games))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách mini game: ${e.message}"))
        }
    }

    override fun getGamesByLesson(lessonId: String): Flow<Resource<List<MiniGame>>> = flow {
        try {
            emit(Resource.Loading())
            val result = firebaseDataSource.getMiniGamesByLesson(lessonId)
            when (result) {
                is Resource.Success -> {
                    val games = result.data ?: emptyList()
                    // Also save to local database
                    games.forEach { game ->
                        miniGameDao.insert(game.toEntity())
                    }
                    emit(Resource.Success(games))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy mini game của bài học: ${e.message}"))
        }
    }

    // Additional methods for complete CRUD operations
    override fun getGamesByTeacher(teacherId: String): Flow<Resource<List<MiniGame>>> = flow {
        try {
            emit(Resource.Loading())
            val games = miniGameDao.getMiniGamesByTeacher(teacherId)
                .map { it.toDomain() }
            emit(Resource.Success(games))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy mini game của giáo viên: ${e.message}"))
        }
    }

    override fun updateGame(game: MiniGame): Flow<Resource<MiniGame>> = flow {
        try {
            emit(Resource.Loading())
            val updatedGame = game.copy(updatedAt = Instant.now())
            miniGameDao.update(updatedGame.toEntity())
            emit(Resource.Success(updatedGame))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi cập nhật mini game: ${e.message}"))
        }
    }

    override fun deleteGame(gameId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            // Delete related questions and options first
            val questions = miniGameQuestionDao.getQuestionsByMiniGame(gameId)
            questions.forEach { question ->
                miniGameOptionDao.deleteOptionsByQuestion(question.id)
            }
            miniGameQuestionDao.deleteQuestionsByMiniGame(gameId)
            
            // Delete the game
            miniGameDao.deleteById(gameId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi xóa mini game: ${e.message}"))
        }
    }

    override fun searchGames(query: String, teacherId: String?): Flow<Resource<List<MiniGame>>> = flow {
        try {
            emit(Resource.Loading())
            val games = miniGameDao.searchMiniGames(query, teacherId)
                .map { it.toDomain() }
            emit(Resource.Success(games))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tìm kiếm mini game: ${e.message}"))
        }
    }

    // Question methods
    override fun createQuestion(question: MiniGameQuestion): Flow<Resource<MiniGameQuestion>> = flow {
        try {
            emit(Resource.Loading())
            
            val questionWithId = if (question.id.isBlank()) {
                question.copy(
                    id = UUID.randomUUID().toString(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            } else {
                question.copy(updatedAt = Instant.now())
            }
            
            // Save to Firebase
            val result = firebaseDataSource.addMiniGameQuestion(questionWithId)
            when (result) {
                is Resource.Success -> {
                    val savedQuestion = result.data ?: questionWithId
                    // Also save to local database
                    miniGameQuestionDao.insert(savedQuestion.toEntity())
                    emit(Resource.Success(savedQuestion))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tạo câu hỏi: ${e.message}"))
        }
    }

    override fun updateQuestion(question: MiniGameQuestion): Flow<Resource<MiniGameQuestion>> = flow {
        try {
            emit(Resource.Loading())
            val updatedQuestion = question.copy(updatedAt = Instant.now())
            miniGameQuestionDao.update(updatedQuestion.toEntity())
            emit(Resource.Success(updatedQuestion))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi cập nhật câu hỏi: ${e.message}"))
        }
    }

    override fun deleteQuestion(questionId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            // Delete related options first
            miniGameOptionDao.deleteOptionsByQuestion(questionId)
            
            // Delete the question
            miniGameQuestionDao.deleteById(questionId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi xóa câu hỏi: ${e.message}"))
        }
    }

    override fun getQuestionById(questionId: String): Flow<Resource<MiniGameQuestion?>> = flow {
        try {
            emit(Resource.Loading())
            val question = miniGameQuestionDao.getQuestionById(questionId)?.toDomain()
            emit(Resource.Success(question))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy câu hỏi: ${e.message}"))
        }
    }
}
