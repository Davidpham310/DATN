package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Test
import com.example.datn.domain.repository.ITestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTestsByClassesUseCase @Inject constructor(
    private val testRepository: ITestRepository
) {
    operator fun invoke(classIds: List<String>): Flow<Resource<List<Test>>> {
        return testRepository.getTestsByClasses(classIds)
    }
}
