package com.example.datn.domain.usecase.minigame

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.MiniGame
import com.example.datn.domain.models.MiniGameQuestion
import com.example.datn.domain.models.MiniGameOption
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentMiniGameAnswer
import com.example.datn.domain.repository.IMiniGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MiniGameUseCases @Inject constructor(
    private val repository: IMiniGameRepository,
    val submitMiniGameResult: SubmitMiniGameResultUseCase,
    val getAllStudentResults: GetAllMiniGameResultsUseCase,
    val getBestResult: GetBestMiniGameResultUseCase,
    val getMiniGameAnswers: GetMiniGameAnswersUseCase
) {
    
    fun createMiniGame(game: MiniGame): Flow<Resource<MiniGame>> {
        return repository.createGame(game)
    }
    
    fun getMiniGameById(gameId: String): Flow<Resource<MiniGame?>> {
        return repository.getGameById(gameId)
    }
    
    fun getMiniGamesByLesson(lessonId: String): Flow<Resource<List<MiniGame>>> {
        return repository.getGamesByLesson(lessonId)
    }
    
    fun getMiniGamesByTeacher(teacherId: String): Flow<Resource<List<MiniGame>>> {
        return repository.getGamesByTeacher(teacherId)
    }
    
    fun getFilteredMiniGames(type: String?, level: String?, teacherId: String? = null): Flow<Resource<List<MiniGame>>> {
        return repository.getFilteredGames(type, level)
    }
    
    fun searchMiniGames(query: String, teacherId: String? = null): Flow<Resource<List<MiniGame>>> {
        return repository.searchGames(query, teacherId)
    }
    
    fun updateMiniGame(game: MiniGame): Flow<Resource<MiniGame>> {
        return repository.updateGame(game)
    }
    
    fun deleteMiniGame(gameId: String): Flow<Resource<Unit>> {
        return repository.deleteGame(gameId)
    }
    
    fun getQuestionsByMiniGame(gameId: String): Flow<Resource<List<MiniGameQuestion>>> {
        return repository.getQuestionsByGame(gameId)
    }
    
    // Question methods
    fun createQuestion(question: MiniGameQuestion): Flow<Resource<MiniGameQuestion>> {
        return repository.createQuestion(question)
    }
    
    fun updateQuestion(question: MiniGameQuestion): Flow<Resource<MiniGameQuestion>> {
        return repository.updateQuestion(question)
    }
    
    fun deleteQuestion(questionId: String): Flow<Resource<Unit>> {
        return repository.deleteQuestion(questionId)
    }
    
    fun getQuestionById(questionId: String): Flow<Resource<MiniGameQuestion?>> {
        return repository.getQuestionById(questionId)
    }

    // Option methods
    fun createOption(option: MiniGameOption): Flow<Resource<MiniGameOption>> {
        return repository.createOption(option)
    }

    fun updateOption(option: MiniGameOption): Flow<Resource<MiniGameOption>> {
        return repository.updateOption(option)
    }

    fun deleteOption(optionId: String): Flow<Resource<Unit>> {
        return repository.deleteOption(optionId)
    }

    fun getOptionsByQuestion(questionId: String): Flow<Resource<List<MiniGameOption>>> {
        return repository.getOptionsByQuestion(questionId)
    }

    fun getOptionById(optionId: String): Flow<Resource<MiniGameOption?>> {
        return repository.getOptionById(optionId)
    }

    fun getStudentResultById(resultId: String): Flow<Resource<StudentMiniGameResult?>> {
        return repository.getStudentResult(resultId)
    }
}
