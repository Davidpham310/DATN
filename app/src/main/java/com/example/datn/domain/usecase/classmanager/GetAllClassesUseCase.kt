package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.Class
import com.example.datn.domain.repository.IClassRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllClassesUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(): Flow<Resource<List<Class>>> {
        return repository.getAllClasses()
    }
}