package com.example.datn.domain.usecase.minigame

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.repository.IMiniGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get the best result (highest score) for a student and game
 * Used to display high score in UI
 */
class GetBestMiniGameResultUseCase @Inject constructor(
    private val repository: IMiniGameRepository
) {
    operator fun invoke(
        studentId: String,
        miniGameId: String
    ): Flow<Resource<StudentMiniGameResult?>> {
        return repository.getBestResult(studentId, miniGameId)
    }
}
