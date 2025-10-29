package com.example.datn.domain.usecase.test

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestOption
import com.example.datn.domain.repository.ITestOptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TestOptionUseCases @Inject constructor(
    private val repository: ITestOptionRepository
) {
    fun create(option: TestOption): Flow<Resource<TestOption>> = repository.createOption(option)
    fun update(option: TestOption): Flow<Resource<TestOption>> = repository.updateOption(option)
    fun delete(optionId: String): Flow<Resource<Unit>> = repository.deleteOption(optionId)
    fun listByQuestion(questionId: String): Flow<Resource<List<TestOption>>> = repository.getOptionsByQuestion(questionId)
    fun getById(optionId: String): Flow<Resource<TestOption?>> = repository.getOptionById(optionId)
}


