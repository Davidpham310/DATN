package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IClassRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import com.example.datn.domain.models.Class

class GetClassByIdUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(classId: String): Flow<Resource<Class?>> {
        return repository.getClassById(classId)
    }
}