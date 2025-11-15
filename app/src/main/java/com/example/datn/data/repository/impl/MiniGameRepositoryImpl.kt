package com.example.datn.data.repository.impl

import com.example.datn.core.network.datasource.FirebaseDataSource
import com.example.datn.core.utils.Resource
import com.example.datn.data.local.dao.MiniGameDao
import com.example.datn.data.local.dao.MiniGameQuestionDao
import com.example.datn.data.local.dao.MiniGameOptionDao
import com.example.datn.data.local.dao.StudentMiniGameResultDao
import com.example.datn.data.local.dao.StudentMiniGameAnswerDao
import com.example.datn.data.local.entities.toEntity
import com.example.datn.data.local.entities.toDomain
import com.example.datn.data.mapper.toDomain
import com.example.datn.data.mapper.toEntity
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentMiniGameAnswer
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
    private val miniGameOptionDao: MiniGameOptionDao,
    private val studentMiniGameResultDao: StudentMiniGameResultDao,
    private val studentMiniGameAnswerDao: StudentMiniGameAnswerDao
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
            // Compute order based on existing questions
            val existing = miniGameQuestionDao.getQuestionsByMiniGame(question.miniGameId)
            val nextOrder = if (existing.isEmpty()) 0 else existing.maxOf { it.order } + 1

            val questionWithId = if (question.id.isBlank()) {
                question.copy(
                    id = UUID.randomUUID().toString(),
                    order = nextOrder,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            } else {
                question.copy(order = if (question.order < 0) nextOrder else question.order, updatedAt = Instant.now())
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
            // Update remote first
            when (val result = firebaseDataSource.updateMiniGameQuestion(updatedQuestion.id, updatedQuestion)) {
                is Resource.Success -> {
                    miniGameQuestionDao.update(updatedQuestion.toEntity())
                    emit(Resource.Success(updatedQuestion))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi cập nhật câu hỏi: ${e.message}"))
        }
    }

    override fun deleteQuestion(questionId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            // Delete remote
            when (val result = firebaseDataSource.deleteMiniGameQuestion(questionId)) {
                is Resource.Success -> {
                    // Delete related options first
                    miniGameOptionDao.deleteOptionsByQuestion(questionId)
                    // Delete the question
                    miniGameQuestionDao.deleteById(questionId)
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
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

    // ==================== OPTION OPERATIONS ====================
    override fun createOption(option: MiniGameOption): Flow<Resource<MiniGameOption>> = flow {
        try {
            emit(Resource.Loading())
            val withId = if (option.id.isBlank()) option.copy(id = UUID.randomUUID().toString()) else option
            val withTimestamps = withId.copy(
                createdAt = if (withId.createdAt.toEpochMilli() == 0L) Instant.now() else withId.createdAt,
                updatedAt = Instant.now()
            )
            val result = firebaseDataSource.addMiniGameOption(withTimestamps)
            when (result) {
                is Resource.Success -> {
                    val saved = result.data ?: withTimestamps
                    miniGameOptionDao.insert(saved.toEntity())
                    emit(Resource.Success(saved))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi tạo đáp án: ${e.message}"))
        }
    }

    override fun updateOption(option: MiniGameOption): Flow<Resource<MiniGameOption>> = flow {
        try {
            emit(Resource.Loading())
            val updated = option.copy(updatedAt = Instant.now())
            when (val result = firebaseDataSource.updateMiniGameOption(updated.id, updated)) {
                is Resource.Success -> {
                    miniGameOptionDao.update(updated.toEntity())
                    emit(Resource.Success(updated))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi cập nhật đáp án: ${e.message}"))
        }
    }

    override fun deleteOption(optionId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.deleteMiniGameOption(optionId)) {
                is Resource.Success -> {
                    miniGameOptionDao.deleteById(optionId)
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi xóa đáp án: ${e.message}"))
        }
    }

    override fun getOptionsByQuestion(questionId: String): Flow<Resource<List<MiniGameOption>>> = flow {
        try {
            emit(Resource.Loading())
            when (val result = firebaseDataSource.getMiniGameOptionsByQuestion(questionId)) {
                is Resource.Success -> {
                    val options = result.data ?: emptyList()
                    options.forEach { miniGameOptionDao.insert(it.toEntity()) }
                    emit(Resource.Success(options))
                }
                is Resource.Error -> emit(Resource.Error(result.message))
                is Resource.Loading -> emit(Resource.Loading())
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy danh sách đáp án: ${e.message}"))
        }
    }

    override fun getOptionById(optionId: String): Flow<Resource<MiniGameOption?>> = flow {
        try {
            emit(Resource.Loading())
            val option = miniGameOptionDao.getOptionById(optionId)?.toDomain()
            emit(Resource.Success(option))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi lấy đáp án: ${e.message}"))
        }
    }

    // ==================== RESULT & ANSWER OPERATIONS ====================

    override fun submitMiniGameResult(
        result: StudentMiniGameResult,
        answers: List<StudentMiniGameAnswer>
    ): Flow<Resource<StudentMiniGameResult>> = flow {
        emit(Resource.Loading())
        try {
            // Get current attempt count
            val count = studentMiniGameResultDao.getAttemptCount(
                result.studentId,
                result.miniGameId
            )

            // Create result with incremented attempt number
            val newResult = result.copy(attemptNumber = count + 1)

            // Save to Room (local database) first for offline support
            studentMiniGameResultDao.insert(newResult.toEntity())
            android.util.Log.d("MiniGameRepo", "✅ Result saved to Room: ${newResult.id}")

            // Save answers to Room
            val answerEntities = answers.map { it.toEntity() }
            studentMiniGameAnswerDao.insertAll(answerEntities)
            android.util.Log.d("MiniGameRepo", "✅ ${answers.size} answers saved to Room")

            // Sync to Firebase
            android.util.Log.d("MiniGameRepo", "🔄 Syncing result to Firebase...")
            when (val firebaseResult = firebaseDataSource.submitMiniGameResult(newResult)) {
                is Resource.Success -> {
                    android.util.Log.d("MiniGameRepo", "✅ Result synced to Firebase: ${newResult.id}")

                    // Sync answers to Firebase
                    android.util.Log.d("MiniGameRepo", "🔄 Syncing ${answers.size} answers to Firebase...")
                    when (val answersResult = firebaseDataSource.saveMiniGameAnswers(answers)) {
                        is Resource.Success -> {
                            android.util.Log.d("MiniGameRepo", "✅ Answers synced to Firebase")
                        }
                        is Resource.Error -> {
                            android.util.Log.w("MiniGameRepo", "⚠️ Failed to sync answers to Firebase: ${answersResult.message}")
                            // Don't fail the whole operation - data is already in Room
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> {
                    android.util.Log.w("MiniGameRepo", "⚠️ Failed to sync result to Firebase: ${firebaseResult.message}")
                    // Don't fail the whole operation - data is already in Room (offline-first approach)
                }
                else -> {}
            }

            emit(Resource.Success(newResult))
        } catch (e: Exception) {
            android.util.Log.e("MiniGameRepo", "❌ Error submitting result: ${e.message}")
            emit(Resource.Error("Error submitting result: ${e.message}"))
        }
    }

    override fun getAllStudentResults(
        studentId: String,
        miniGameId: String
    ): Flow<Resource<List<StudentMiniGameResult>>> = flow {
        emit(Resource.Loading())
        try {
            val entities = studentMiniGameResultDao
                .getResultsByStudentAndGame(studentId, miniGameId)
            val results = entities.map { it.toDomain() }
            emit(Resource.Success(results))
        } catch (e: Exception) {
            emit(Resource.Error("Error loading results: ${e.message}"))
        }
    }

    override fun getStudentResult(
        resultId: String
    ): Flow<Resource<StudentMiniGameResult?>> = flow {
        emit(Resource.Loading())
        try {
            val entity = studentMiniGameResultDao.getResultById(resultId)
            val result = entity?.toDomain()
            emit(Resource.Success(result))
        } catch (e: Exception) {
            emit(Resource.Error("Error loading result: ${e.message}"))
        }
    }

    override fun getLatestResult(
        studentId: String,
        miniGameId: String
    ): Flow<Resource<StudentMiniGameResult?>> = flow {
        emit(Resource.Loading())
        try {
            val entity = studentMiniGameResultDao
                .getLatestResult(studentId, miniGameId)
            val result = entity?.toDomain()
            emit(Resource.Success(result))
        } catch (e: Exception) {
            emit(Resource.Error("Error loading latest result: ${e.message}"))
        }
    }

    override fun getBestResult(
        studentId: String,
        miniGameId: String
    ): Flow<Resource<StudentMiniGameResult?>> = flow {
        emit(Resource.Loading())
        try {
            val entity = studentMiniGameResultDao
                .getBestResult(studentId, miniGameId)
            val result = entity?.toDomain()
            emit(Resource.Success(result))
        } catch (e: Exception) {
            emit(Resource.Error("Error loading best result: ${e.message}"))
        }
    }

    override fun getStudentAnswers(
        resultId: String
    ): Flow<Resource<List<StudentMiniGameAnswer>>> = flow {
        emit(Resource.Loading())
        try {
            val entities = studentMiniGameAnswerDao
                .getAnswersByResultId(resultId)
            val answers = entities.map { it.toDomain() }
            emit(Resource.Success(answers))
        } catch (e: Exception) {
            emit(Resource.Error("Error loading answers: ${e.message}"))
        }
    }
}
