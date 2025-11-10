package com.example.datn.domain.usecase.minigame

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.StudentMiniGameAnswer
import com.example.datn.domain.repository.IMiniGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get all answers for a specific result
 * Used to display answer details in result screen
 */
class GetMiniGameAnswersUseCase @Inject constructor(
    private val repository: IMiniGameRepository
) {
    operator fun invoke(
        resultId: String
    ): Flow<Resource<List<StudentMiniGameAnswer>>> {
        return repository.getStudentAnswers(resultId)
    }
}
