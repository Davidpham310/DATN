package com.example.datn.domain.repository

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.TestOption
import kotlinx.coroutines.flow.Flow

interface ITestOptionRepository {
    fun createOption(option: TestOption): Flow<Resource<TestOption>>
    fun updateOption(option: TestOption): Flow<Resource<TestOption>>
    fun deleteOption(optionId: String): Flow<Resource<Unit>>
    fun getOptionsByQuestion(questionId: String): Flow<Resource<List<TestOption>>>
    fun getOptionById(optionId: String): Flow<Resource<TestOption?>>
}


