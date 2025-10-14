package com.example.datn.domain.usecase.classmanager

import com.example.datn.core.utils.Resource
import com.example.datn.domain.repository.IClassRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteClassUseCase @Inject constructor(
    private val repository: IClassRepository
) {
    operator fun invoke(classId: String): Flow<Resource<Unit>> {
        return repository.deleteClass(classId)
    }
}