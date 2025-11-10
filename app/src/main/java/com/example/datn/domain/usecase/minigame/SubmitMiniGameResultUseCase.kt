package com.example.datn.domain.usecase.minigame

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentMiniGameResult
import com.example.datn.domain.models.StudentMiniGameAnswer
import com.example.datn.domain.repository.IMiniGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to submit a mini game result with answers
 * Automatically increments attempt number for unlimited replay
 */
class SubmitMiniGameResultUseCase @Inject constructor(
    private val repository: IMiniGameRepository
) {
    operator fun invoke(
        result: StudentMiniGameResult,
        answers: List<StudentMiniGameAnswer>
    ): Flow<Resource<StudentMiniGameResult>> {
        return repository.submitMiniGameResult(result, answers)
    }
}
