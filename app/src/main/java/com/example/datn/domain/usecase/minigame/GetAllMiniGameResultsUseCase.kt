package com.example.datn.domain.usecase.minigame

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.repository.IMiniGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get ALL results for a student and game (unlimited replay)
 * Returns results ordered by attempt number descending (newest first)
 */
class GetAllMiniGameResultsUseCase @Inject constructor(
    private val repository: IMiniGameRepository
) {
    operator fun invoke(
        studentId: String,
        miniGameId: String
    ): Flow<Resource<List<StudentMiniGameResult>>> {
        return repository.getAllStudentResults(studentId, miniGameId)
    }
}
